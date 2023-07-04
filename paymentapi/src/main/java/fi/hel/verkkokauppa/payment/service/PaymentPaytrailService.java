package fi.hel.verkkokauppa.payment.service;

import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.SendEventService;
import fi.hel.verkkokauppa.common.events.TopicName;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.EncryptorUtil;
import fi.hel.verkkokauppa.common.util.StringUtils;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.payment.api.data.*;
import fi.hel.verkkokauppa.payment.constant.PaymentGatewayEnum;
import fi.hel.verkkokauppa.payment.mapper.PaytrailPaymentProviderListMapper;
import fi.hel.verkkokauppa.payment.model.Payer;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.PaymentItem;
import fi.hel.verkkokauppa.payment.model.PaymentStatus;
import fi.hel.verkkokauppa.payment.model.paytrail.payment.PaytrailPaymentProviderModel;
import fi.hel.verkkokauppa.payment.paytrail.PaytrailPaymentClient;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContextBuilder;
import fi.hel.verkkokauppa.payment.repository.PayerRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentItemRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentRepository;
import fi.hel.verkkokauppa.payment.util.PaymentUtil;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.PaytrailClient;
import org.helsinki.paytrail.constants.CheckoutAlgorithm;
import org.helsinki.paytrail.constants.CheckoutMethod;
import org.helsinki.paytrail.model.paymentmethods.PaytrailPaymentMethod;
import org.helsinki.paytrail.model.payments.PaytrailPaymentMitChargeSuccessResponse;
import org.helsinki.paytrail.model.payments.PaytrailPaymentResponse;
import org.helsinki.paytrail.model.tokenization.PaytrailTokenResponse;
import org.helsinki.paytrail.service.PaytrailSignatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;


@Service
@Slf4j
public class PaymentPaytrailService {

    private final PaytrailPaymentClient paytrailPaymentClient;
    private final PaytrailPaymentContextBuilder paymentContextBuilder;
    private final PaymentRepository paymentRepository;
    private final PayerRepository payerRepository;
    private final PaymentItemRepository paymentItemRepository;
    private final PaytrailPaymentProviderListMapper paytrailPaymentProviderListMapper;

    @Value("${payment.card_token.encryption.password}")
    private String cardTokenEncryptionPassword;

    @Autowired
    private SendEventService sendEventService;

    @Autowired
    private CommonServiceConfigurationClient commonServiceConfigurationClient;

    @Autowired
    private SendNotificationService sendNotificationService;

    @Autowired
    PaymentPaytrailService(
            PaytrailPaymentClient paytrailPaymentClient,
            CommonServiceConfigurationClient commonServiceConfigurationClient,
            PaytrailPaymentContextBuilder paymentContextBuilder,
            PaymentRepository paymentRepository,
            PayerRepository payerRepository,
            PaymentItemRepository paymentItemRepository,
            PaytrailPaymentProviderListMapper paytrailPaymentProviderListMapper) {
        this.paytrailPaymentClient = paytrailPaymentClient;
        this.paymentContextBuilder = paymentContextBuilder;
        this.paymentRepository = paymentRepository;
        this.payerRepository = payerRepository;
        this.paymentItemRepository = paymentItemRepository;
        this.paytrailPaymentProviderListMapper = paytrailPaymentProviderListMapper;
    }


    public PaymentMethodDto[] getOnlinePaymentMethodList(String merchantId, String namespace, String currency) {
        if (StringUtils.isNotEmpty(merchantId)) {
            try {
                PaytrailPaymentContext context = paymentContextBuilder.buildFor(namespace, merchantId, false);
                List<PaytrailPaymentMethod> paymentMethods = paytrailPaymentClient.getPaymentMethods(context);
                return paymentMethods.stream().map(paymentMethod -> new PaymentMethodDto(
                        paymentMethod.getName(),
                        paymentMethod.getId(),
                        paymentMethod.getGroup(),
                        paymentMethod.getIcon(),
                        PaymentGatewayEnum.PAYTRAIL
                )).toArray(PaymentMethodDto[]::new);
            } catch (CommonApiException e) {
                log.debug("Something went wrong in payment method fetching");
                Error error = e.getErrors().getErrors().get(0);
                log.debug(error.getCode());
                log.debug(error.getMessage());
                return new PaymentMethodDto[0];
            }
        } else {
            log.debug("merchantId cannot be null or empty!");
            return new PaymentMethodDto[0];
        }
    }

    public Payment getPaymentRequestData(GetPaymentRequestDataDto dto) {
        OrderDto order = dto.getOrder().getOrder();
        String namespace = order.getNamespace();
        String merchantId = dto.getMerchantId();
        String orderId = order.getOrderId();
        String orderStatus = order.getStatus();
        String userId = order.getUser();

        isValidOrderStatusToCreatePayment(orderId, orderStatus);
        isValidUserToCreatePayment(orderId, userId);

        boolean isRecurringOrder = order.getType().equals(OrderType.SUBSCRIPTION);
        String paymentType = isRecurringOrder ? OrderType.SUBSCRIPTION : OrderType.ORDER;

        String paymentId = PaymentUtil.generatePaymentOrderNumber(order.getOrderId());
        PaytrailPaymentContext context = paymentContextBuilder.buildFor(namespace, merchantId, false);
        PaytrailPaymentResponse createResponse = paytrailPaymentClient.createPayment(context, paymentId, dto.getOrder());

        Payment payment = createPayment(context, dto, paymentType, paymentId, createResponse);
        if (payment.getPaymentId() == null) {
            throw new RuntimeException("Didn't manage to create paytrail payment.");
        }

        return payment;
    }

    public PaytrailPaymentContext buildPaytrailContext(String namespace, String merchantId) throws CommonApiException {
        return paymentContextBuilder.buildFor(namespace, merchantId, false);
    }

    public PaytrailTokenResponse getToken(PaytrailPaymentContext context, String tokenizationId) throws ExecutionException, InterruptedException {
        return paytrailPaymentClient.getToken(context, tokenizationId);
    }

    public PaytrailPaymentMitChargeSuccessResponse createMitCharge(PaytrailPaymentContext context, String paymentId, OrderWrapper order, String token) throws ExecutionException, InterruptedException {
        return paytrailPaymentClient.createMitCharge(context, paymentId, order, token);
    }

    public PaytrailPaymentMitChargeSuccessResponse createMitCharge(PaytrailPaymentContext context, String paymentId, OrderMessage message) throws ExecutionException, InterruptedException {
        String decryptedToken = EncryptorUtil.decryptValue(message.getCardToken(), cardTokenEncryptionPassword);
        return paytrailPaymentClient.createMitCharge(context, paymentId, message, decryptedToken);
    }

    private void isValidUserToCreatePayment(String orderId, String userId) {
        if (userId == null || userId.isEmpty()) {
            log.warn("creating paytrail payment without user rejected, orderId: " + orderId);
            throw new CommonApiException(
                    HttpStatus.FORBIDDEN,
                    new Error("rejected-creating-paytrail-payment-for-order-without-user", "rejected creating paytrail payment for order without user, order id [" + orderId + "]")
            );
        }
    }

    private void isValidOrderStatusToCreatePayment(String orderId, String orderStatus) {
        // check order status, can only create payment for confirmed orders
        if (!"confirmed".equals(orderStatus)) {
            log.warn("creating paytrail payment for unconfirmed order rejected, orderId: " + orderId);
            throw new CommonApiException(
                    HttpStatus.FORBIDDEN,
                    new Error("rejected-creating-paytrail-payment-for-unconfirmed-order", "rejected creating paytrail payment for unconfirmed order, order id [" + orderId + "]")
            );
        }
    }

    public void validateOrder(OrderDto order) {
        isValidOrderStatusToCreatePayment(order.getOrderId(), order.getStatus());
        isValidUserToCreatePayment(order.getOrderId(), order.getUser());
    }

    private Payment createPayment(PaytrailPaymentContext context, GetPaymentRequestDataDto dto, String type, String paymentId) {
        OrderDto order = dto.getOrder().getOrder();
        List<OrderItemDto> items = dto.getOrder().getItems();

        if (items.isEmpty()) {
            throw new IllegalArgumentException("Items cannot be empty.");
        }

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");

        String namespace = order.getNamespace();

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setNamespace(order.getNamespace());
        payment.setOrderId(order.getOrderId());
        payment.setUserId(order.getUser());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setPaymentMethodLabel(dto.getPaymentMethodLabel());
        payment.setTimestamp(sdf.format(timestamp));
        payment.setAdditionalInfo("{\"payment_method\": " + dto.getPaymentMethod() + "}");
        payment.setPaymentType(type);
        payment.setStatus(PaymentStatus.CREATED);
        payment.setTotalExclTax(new BigDecimal(order.getPriceNet()));
        payment.setTaxAmount(new BigDecimal(order.getPriceVat()));
        payment.setTotal(new BigDecimal(order.getPriceTotal()));
        payment.setShopInShopPayment(context.isUseShopInShop());
        payment.setPaymentGateway(PaymentGatewayEnum.PAYTRAIL);

        createPayer(order, paymentId);

        for (OrderItemDto item : items) {
            createPaymentItem(item, paymentId, order.getOrderId());
        }

        log.debug("creating payment for namespace: " + namespace + " with paymentId: " + paymentId);

        return payment;
    }

    private Payment createPayment(PaytrailPaymentContext context, GetPaymentRequestDataDto dto, String type, String paymentId, PaytrailPaymentResponse paymentResponse) {
        Payment payment = createPayment(context, dto, type, paymentId);

        payment.setPaytrailTransactionId(paymentResponse.getTransactionId());
        List<PaytrailPaymentProviderModel> providers = paytrailPaymentProviderListMapper.fromDto(paymentResponse.getProviders());

        PaytrailPaymentProviderModel provider = providers
                .stream()
                .filter(providerModel -> Objects.equals(providerModel.getId(), dto.getPaymentMethod()))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("paytrail-payment-providers {}", paymentResponse.getProviders());
                    return new CommonApiException(
                            HttpStatus.NOT_FOUND,
                            new Error("paytrail-payment-provider-not-found", "Cant find paytrail payment provider to orderId [" + dto.getOrder().getOrder().getOrderId() + "] ")
                    );
                });

        payment.setPaytrailProvider(provider);

        paymentRepository.save(payment);
        log.debug("created payment with paymentId: " + paymentId);

        return payment;
    }

    public Payment createPayment(PaytrailPaymentContext context, GetPaymentRequestDataDto dto, String paymentId, PaytrailPaymentMitChargeSuccessResponse paymentResponse) {
        boolean isRecurringOrder = dto.getOrder().getOrder().getType().equals(OrderType.SUBSCRIPTION);
        String paymentType = isRecurringOrder ? OrderType.SUBSCRIPTION : OrderType.ORDER;

        Payment payment = createPayment(context, dto, paymentType, paymentId);
        payment.setPaytrailTransactionId(paymentResponse.getTransactionId());
        payment.setStatus(PaymentStatus.PAID_ONLINE);

        paymentRepository.save(payment);
        log.debug("created payment with paymentId: " + paymentId);

        return payment;
    }

    private void createPaymentItem(OrderItemDto itemDto, String paymentId, String orderId) {
        PaymentItem item = new PaymentItem();
        item.setPaymentId(paymentId);
        item.setOrderId(orderId);
        item.setProductId(itemDto.getProductId());
        item.setProductName(itemDto.getProductName());
        item.setQuantity(item.getQuantity());
        item.setRowPriceNet(itemDto.getRowPriceNet());
        item.setRowPriceVat(itemDto.getRowPriceVat());
        item.setRowPriceTotal(itemDto.getRowPriceTotal());
        item.setTaxPercent(itemDto.getVatPercentage());
        item.setPriceNet(itemDto.getPriceNet());
        item.setTaxAmount(itemDto.getPriceVat());
        item.setPriceGross(itemDto.getPriceGross());

        paymentItemRepository.save(item);
    }

    private void createPayer(OrderDto orderDto, String paymentId) {
        Payer payer = new Payer();
        payer.setPaymentId(paymentId);
        payer.setFirstName(orderDto.getCustomerFirstName());
        payer.setLastName(orderDto.getCustomerLastName());
        payer.setEmail(orderDto.getCustomerEmail());

        payerRepository.save(payer);
    }

    public String getPaymentUrl(String transactionId) {
        return PaytrailClient.PAYMENT_UI_URL + "/pay/" + transactionId;
    }

    public String getPaymentUrl(Payment payment) {
        return getPaymentUrl(payment.getPaytrailTransactionId());
    }

    public void triggerPaymentPaidEvent(Payment payment, PaytrailTokenResponse card) {
        String now = DateTimeUtil.getDateTime();
        String encryptedToken = EncryptorUtil.encryptValue(card.getToken(), cardTokenEncryptionPassword);

        PaymentMessage paymentMessage = PaymentMessage.builder()
                .eventType(EventType.PAYMENT_PAID)
                .eventTimestamp(now)
                .namespace(payment.getNamespace())
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .paymentPaidTimestamp(now)
                .orderType(payment.getPaymentType())
                .encryptedCardToken(encryptedToken)
                .cardTokenExpYear(Short.parseShort(card.getCard().getExpireYear()))
                .cardTokenExpMonth(Byte.parseByte(card.getCard().getExpireMonth()))
                .cardLastFourDigits(card.getCard().getPartialPan())
                .paymentGateway(fi.hel.verkkokauppa.common.constants.PaymentGatewayEnum.PAYTRAIL)
                .build();

        // send PAYMENT_PAID webhook event
        orderPaidWebHookAction(paymentMessage);
        log.debug("PAYMENT_PAID notification sent for paymentId: " + payment.getPaymentId());

        // if orderType equals subscription then this subscription has just been created
        if(OrderType.SUBSCRIPTION.equals(paymentMessage.getOrderType())){
            // send SUBSCRIPTION_CREATED webhook event
            sendEventService.sendEventMessage(TopicName.SUBSCRIPTIONS, paymentMessage);
            log.debug("triggered event SUBSCRIPTION_CREATED for paymentId: " + payment.getPaymentId());
        }

    }

    protected void orderPaidWebHookAction(PaymentMessage message) {
        try {
            sendNotificationService.sendPaymentMessageNotification(message);
        } catch (Exception e) {
            log.error("webhookAction: failed action after receiving event, eventType: " + message.getEventType(), e);
        }
    }

    public void triggerCardUpdateEvent(OrderDto order, PaytrailTokenResponse card) {
        String now = DateTimeUtil.getDateTime();
        String encryptedToken = EncryptorUtil.encryptValue(card.getToken(), cardTokenEncryptionPassword);

        SubscriptionMessage message = SubscriptionMessage.builder()
                .eventType(EventType.SUBSCRIPTION_UPDATE_CARD)
                .timestamp(now)
                .subscriptionId(order.getSubscriptionId())
                .orderId(order.getOrderId())
                .orderType(order.getType())
                .encryptedCardToken(encryptedToken)
                .cardTokenExpYear(card.getCard().getExpireYear())
                .cardTokenExpMonth(card.getCard().getExpireMonth())
                .cardLastFourDigits(card.getCard().getPartialPan())
                .user(order.getUser())
                .build();

        sendEventService.sendEventMessage(TopicName.SUBSCRIPTIONS, message);
        log.debug("triggered event SUBSCRIPTION_UPDATE_CARD for subscriptionId: " + order.getSubscriptionId());
    }

    public TreeMap<String, String> getCardReturnParameters(
            String merchantId,
            String namespace,
            String orderId
    ) throws NoSuchAlgorithmException, InvalidKeyException, CommonApiException {
        PaytrailPaymentContext context = paymentContextBuilder.buildFor(namespace, merchantId, false);

        String redirectSuccessUrl = context.getCardRedirectSuccessUrl();
        String redirectCancelUrl = context.getCardRedirectCancelUrl();
        String callbackSuccessUrl = context.getCardCallbackSuccessUrl();
        String callbackCancelUrl = context.getCardCallbackCancelUrl();

        TreeMap<String, String> parameters = new TreeMap<>();
        parameters.put("checkout-account", context.getPaytrailMerchantId());
        parameters.put("checkout-algorithm", CheckoutAlgorithm.SHA256.toString());
        parameters.put("checkout-method", CheckoutMethod.POST.toString());
        parameters.put("checkout-nonce", UUIDGenerator.generateType4UUID().toString());
        parameters.put("checkout-timestamp", Instant.now().toString());
        parameters.put("checkout-redirect-success-url", redirectSuccessUrl + (redirectSuccessUrl.endsWith("/") ? "" : "/") + orderId);
        parameters.put("checkout-redirect-cancel-url", redirectCancelUrl + (redirectCancelUrl.endsWith("/") ? "" : "/") + orderId);
        parameters.put("checkout-callback-success-url", callbackSuccessUrl + (callbackSuccessUrl.endsWith("/") ? "" : "/") + orderId);
        parameters.put("checkout-callback-cancel-url", callbackCancelUrl + (callbackCancelUrl.endsWith("/") ? "" : "/") + orderId);
        parameters.put("signature", PaytrailSignatureService.calculateSignature(parameters, null, context.getPaytrailSecretKey()));

        return parameters;
    }

    public TreeMap<String, String> getUpdateCardReturnParameters(
            String merchantId,
            String namespace,
            String orderId
    ) throws NoSuchAlgorithmException, InvalidKeyException {
        PaytrailPaymentContext context = paymentContextBuilder.buildFor(namespace, merchantId, false);

        String redirectSuccessUrl = context.getUpdateCardRedirectSuccessUrl();
        String redirectCancelUrl = context.getUpdateCardRedirectCancelUrl();
        String callbackSuccessUrl = context.getUpdateCardCallbackSuccessUrl();
        String callbackCancelUrl = context.getUpdateCardCallbackCancelUrl();

        TreeMap<String, String> parameters = new TreeMap<>();
        parameters.put("checkout-account", context.getPaytrailMerchantId());
        parameters.put("checkout-algorithm", CheckoutAlgorithm.SHA256.toString());
        parameters.put("checkout-method", CheckoutMethod.POST.toString());
        parameters.put("checkout-nonce", UUIDGenerator.generateType4UUID().toString());
        parameters.put("checkout-timestamp", Instant.now().toString());
        parameters.put("checkout-redirect-success-url", redirectSuccessUrl + (redirectSuccessUrl.endsWith("/") ? "" : "/") + orderId);
        parameters.put("checkout-redirect-cancel-url", redirectCancelUrl + (redirectCancelUrl.endsWith("/") ? "" : "/") + orderId);
        parameters.put("checkout-callback-success-url", callbackSuccessUrl + (callbackSuccessUrl.endsWith("/") ? "" : "/") + orderId);
        parameters.put("checkout-callback-cancel-url", callbackCancelUrl + (callbackCancelUrl.endsWith("/") ? "" : "/") + orderId);
        parameters.put("signature", PaytrailSignatureService.calculateSignature(parameters, null, context.getPaytrailSecretKey()));

        return parameters;
    }
}

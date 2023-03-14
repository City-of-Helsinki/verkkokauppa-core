package fi.hel.verkkokauppa.payment.service;

import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.constants.PaymentType;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.SendEventService;
import fi.hel.verkkokauppa.common.events.TopicName;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.EncryptorUtil;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.payment.api.data.*;
import fi.hel.verkkokauppa.payment.constant.PaymentGatewayEnum;
import fi.hel.verkkokauppa.payment.logic.builder.CardTokenPayloadBuilder;
import fi.hel.verkkokauppa.payment.logic.builder.PaymentContextBuilder;
import fi.hel.verkkokauppa.payment.logic.builder.PaymentOnlyChargeCardPayloadBuilder;
import fi.hel.verkkokauppa.payment.logic.builder.PaymentTokenPayloadBuilder;
import fi.hel.verkkokauppa.payment.logic.context.PaymentContext;
import fi.hel.verkkokauppa.payment.logic.fetcher.CardTokenFetcher;
import fi.hel.verkkokauppa.payment.logic.fetcher.ChargeCardTokenFetcher;
import fi.hel.verkkokauppa.payment.logic.fetcher.PaymentTokenFetcher;
import fi.hel.verkkokauppa.payment.util.PaymentUtil;
import fi.hel.verkkokauppa.payment.model.Payer;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.PaymentItem;
import fi.hel.verkkokauppa.payment.model.PaymentStatus;
import fi.hel.verkkokauppa.payment.repository.PayerRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentItemRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentRepository;
import org.helsinki.vismapay.VismaPayClient;
import org.helsinki.vismapay.request.payment.ChargeCardTokenRequest;
import org.helsinki.vismapay.request.payment.ChargeRequest;
import org.helsinki.vismapay.response.payment.ChargeCardTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class OnlinePaymentService {

    private final Logger log = LoggerFactory.getLogger(OnlinePaymentService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PayerRepository payerRepository;

    @Autowired
    private PaymentItemRepository paymentItemRepository;

    @Autowired
    private PaymentTokenPayloadBuilder paymentTokenPayloadBuilder;

    @Autowired
    private PaymentOnlyChargeCardPayloadBuilder onlyChargeCardPayloadBuilder;

    @Autowired
    private PaymentTokenFetcher paymentTokenFetcher;

    @Autowired
    private CardTokenFetcher cardTokenFetcher;

    @Autowired
    private ChargeCardTokenFetcher chargeCardTokenFetcher;

    @Autowired
    private CardTokenPayloadBuilder cardTokenPayloadBuilder;

    @Autowired
    private PaymentContextBuilder paymentContextBuilder;

    @Autowired
    private SendEventService sendEventService;

    @Value("${payment.card_token.encryption.password}")
    private String cardTokenEncryptionPassword;

    @Autowired
    private SendNotificationService sendNotificationService;


    public Payment getPaymentRequestData(GetPaymentRequestDataDto dto) {
        OrderDto order = dto.getOrder().getOrder();
        String namespace = order.getNamespace();
        String orderId = order.getOrderId();
        String orderStatus = order.getStatus();
        String userId = order.getUser();

        isValidOrderStatusToCreatePayment(orderId, orderStatus);

        isValidUserToCreatePayment(orderId, userId);

        boolean isRecurringOrder = order.getType().equals(OrderType.SUBSCRIPTION);
        String paymentType = isRecurringOrder ? OrderType.SUBSCRIPTION : OrderType.ORDER;

        PaymentContext context = paymentContextBuilder.buildFor(namespace);

        ChargeRequest.PaymentTokenPayload tokenRequestPayload = paymentTokenPayloadBuilder.buildFor(dto, context);
        log.debug("tokenRequestPayload: " + tokenRequestPayload);

        String paymentId = tokenRequestPayload.getOrderNumber();
        String token = paymentTokenFetcher.getToken(tokenRequestPayload);

        Payment payment = createPayment(dto, paymentType, token, paymentId);
        if (payment.getPaymentId() == null) {
            throw new RuntimeException("Didn't manage to create payment.");
        }

        return payment;
    }

    public Payment getPaymentRequestDataForCardRenewal(GetPaymentRequestDataDto dto) {
        // Gets orderDto from order wrapper
        OrderDto order = dto.getOrder().getOrder();
        String namespace = order.getNamespace();
        String orderId = order.getOrderId();
        String orderStatus = order.getStatus();
        String userId = order.getUser();

        isValidOrderStatusToCreatePayment(orderId, orderStatus);

        isValidUserToCreatePayment(orderId, userId);

        PaymentContext context = paymentContextBuilder.buildFor(namespace);

        ChargeRequest.PaymentTokenPayload tokenRequestPayload = onlyChargeCardPayloadBuilder.buildFor(dto, context);
        log.debug("tokenRequestPayload: " + tokenRequestPayload);

        String paymentId = tokenRequestPayload.getOrderNumber();
        String token = paymentTokenFetcher.getToken(tokenRequestPayload);
        String paymentType = PaymentType.CARD_RENEWAL;

        Payment payment = createPayment(dto, paymentType, token, paymentId);
        if (payment.getPaymentId() == null) {
            throw new RuntimeException("Didn't manage to create payment.");
        }

        return payment;
    }

    private void isValidUserToCreatePayment(String orderId, String userId) {
        if (userId == null || userId.isEmpty()) {
            log.warn("creating payment without user rejected, orderId: " + orderId);
            throw new CommonApiException(
                    HttpStatus.FORBIDDEN,
                    new Error("rejected-creating-payment-for-order-without-user", "rejected creating payment for order without user, order id [" + orderId + "]")
            );
        }
    }

    private void isValidOrderStatusToCreatePayment(String orderId, String orderStatus) {
        // check order status, can only create payment for confirmed orders
        if (!"confirmed".equals(orderStatus)) {
            log.warn("creating payment for unconfirmed order rejected, orderId: " + orderId);
            throw new CommonApiException(
                    HttpStatus.FORBIDDEN,
                    new Error("rejected-creating-payment-for-unconfirmed-order", "rejected creating payment for unconfirmed order, order id [" + orderId + "]")
            );
        }
    }


    public String getPaymentUrl(String token) {
        return VismaPayClient.API_URL + "/token/" + token;
    }

    public String getPaymentUrl(Payment payment) {
        return getPaymentUrl(payment.getToken());
    }

    public void setPaymentStatus(String paymentId, String status) {
        Payment payment = getPayment(paymentId);
        payment.setStatus(status);
        paymentRepository.save(payment);
    }

    public void setPaytrailTransactionId(String paymentId, String transactionId) {
        Payment payment = getPayment(paymentId);
        payment.setPaytrailTransactionId(transactionId);
        paymentRepository.save(payment);
    }

    public Payment getPaymentForOrder(String orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(orderId);

        Payment paidPayment = selectPaidPayment(payments);
        Payment payablePayment = selectPayablePayment(payments);

        if (paidPayment != null)
            return paidPayment;
        else if (payablePayment != null)
            return payablePayment;
        else
            return null;
    }

    public List<Payment> getPaymentsForOrder(String orderId, String namepace) {
        return paymentRepository.findByNamespaceAndOrderId(namepace,orderId);
    }

    public List<Payment> getPaymentsWithNamespaceAndOrderIdAndStatus(String orderId, String namepace, String status) {
        return paymentRepository.findByNamespaceAndOrderIdAndStatus(namepace, orderId, status);
    }

    // payment precedence selection from KYV-186
    private Payment selectPayablePayment(List<Payment> payments) {
        Payment payablePayment = null;

        if (payments != null)
            for (Payment payment : payments) {
                // in an unpayable state
                if (payment.getStatus() == PaymentStatus.PAID_ONLINE || payment.getStatus() == PaymentStatus.CANCELLED)
                    continue;

                // an earlier selected payment is newer
                if (payablePayment != null && payablePayment.getTimestamp().compareTo(payment.getTimestamp()) > 0)
                    continue;

                payablePayment = payment;
            }

        return payablePayment;
    }

    private Payment selectPaidPayment(List<Payment> payments) {
        if (payments != null)
            for (Payment payment : payments) {
                if (payment.getStatus() == PaymentStatus.PAID_ONLINE)
                    return payment;
            }

        return null;
    }

    public Payment getPayment(String paymentId) {
        Optional<Payment> payment = paymentRepository.findById(paymentId);

        if (!payment.isPresent()) {
            log.debug("payment not found, paymentId: " + paymentId);
            Error error = new Error("payment-not-found-from-backend", "payment with payment id [" + paymentId + "] not found from backend");
            throw new CommonApiException(HttpStatus.NOT_FOUND, error);
        }

        return payment.get();
    }

    public PaymentCardInfoDto getPaymentCardToken(String paymentId) {
        return cardTokenFetcher.getCardToken(paymentId);
    }

    private Payment createPayment(GetPaymentRequestDataDto dto, String type, String token, String paymentId) {
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
        payment.setToken(token);
        payment.setTotalExclTax(new BigDecimal(order.getPriceNet()));
        payment.setTaxAmount(new BigDecimal(order.getPriceVat()));
        payment.setTotal(new BigDecimal(order.getPriceTotal()));
        payment.setPaymentGateway(PaymentGatewayEnum.VISMA);

        createPayer(order);

        for (OrderItemDto item : items) {
            createPaymentItem(item, paymentId, order.getOrderId());
        }

        paymentRepository.save(payment);
        log.debug("created payment for namespace: " + namespace + " with paymentId: " + paymentId);

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

    private void createPayer(OrderDto orderDto) {
        Payer payer = new Payer();
        payer.setFirstName(orderDto.getCustomerFirstName());
        payer.setLastName(orderDto.getCustomerLastName());
        payer.setEmail(orderDto.getCustomerEmail());

        payerRepository.save(payer);
    }

    public Payment createSubscriptionRenewalPayment(OrderMessage message) {

        String paymentId = PaymentUtil.generatePaymentOrderNumber(message.getOrderId());

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");

        String namespace = message.getNamespace();

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setNamespace(message.getNamespace());
        payment.setOrderId(message.getOrderId());
        payment.setUserId(message.getUserId());
        payment.setPaymentMethod(PaymentType.CREDIT_CARDS);
        payment.setTimestamp(sdf.format(timestamp));
        payment.setPaymentType(OrderType.ORDER);
        payment.setStatus(PaymentStatus.CREATED);
        payment.setTotalExclTax(new BigDecimal(message.getPriceNet()));
        payment.setTaxAmount(new BigDecimal(message.getPriceVat()));
        payment.setTotal(new BigDecimal(message.getPriceTotal()));
        paymentRepository.save(payment);

        OrderItemDto item = new OrderItemDto();
        item.setOrderItemId(message.getOrderItemId());
        item.setProductName(message.getProductName());
        item.setQuantity(Integer.parseInt(message.getProductQuantity()));
        item.setRowPriceTotal(new BigDecimal(message.getPriceTotal()));
        item.setRowPriceNet(new BigDecimal(message.getPriceNet()));
        item.setVatPercentage(message.getVatPercentage());
        createPaymentItem(item, paymentId, message.getOrderId());

        log.debug("created subscription renewal payment for namespace: " + namespace + " with paymentId: " + paymentId);

        return payment;
    }

    public ChargeCardTokenResponse chargeCardToken(ChargeCardTokenRequestDataDto request, Payment payment) {
        PaymentContext context = paymentContextBuilder.buildFor(request.getNamespace());
        ChargeCardTokenRequest.CardTokenPayload payload = cardTokenPayloadBuilder.buildFor(context, request);
        log.debug("ChargeCardTokenRequest payload: {}",payload);
        return chargeCardTokenFetcher.chargeCardToken(payload, payment);
    }

    public PaymentCardInfoDto getPaymentCardInfo(String namespace, String orderId, String userId) {
        Payment payment = findByIdValidateByUser(namespace, orderId, userId);
        String paymentId = payment.getPaymentId();
        PaymentCardInfoDto paymentCardToken = getPaymentCardToken(paymentId);

        return paymentCardToken;
    }

    public void triggerPaymentPaidEvent(Payment payment, PaymentCardInfoDto card) {
        String now = DateTimeUtil.getDateTime();

        PaymentMessage.PaymentMessageBuilder paymentMessageBuilder = PaymentMessage.builder()
                .eventType(EventType.PAYMENT_PAID)
                .eventTimestamp(now)
                .namespace(payment.getNamespace())
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .paymentPaidTimestamp(now)
                .orderType(payment.getPaymentType());

        if (PaymentType.CREDIT_CARDS.equalsIgnoreCase(payment.getPaymentMethod())) {
            if (card != null) {
                paymentMessageBuilder
                        .encryptedCardToken(card.getCardToken())
                        .cardTokenExpYear(card.getExpYear())
                        .cardTokenExpMonth(card.getExpMonth())
                        .cardLastFourDigits(card.getCardLastFourDigits());
            } else {
                PaymentCardInfoDto paymentCardInfo = getPaymentCardInfo(payment.getNamespace(), payment.getOrderId(), payment.getUserId());

                if (paymentCardInfo != null) {
                    String encryptedToken = EncryptorUtil.encryptValue(paymentCardInfo.getCardToken(), cardTokenEncryptionPassword);

                    paymentMessageBuilder
                            .encryptedCardToken(encryptedToken)
                            .cardTokenExpYear(paymentCardInfo.getExpYear())
                            .cardTokenExpMonth(paymentCardInfo.getExpMonth())
                            .cardLastFourDigits(paymentCardInfo.getCardLastFourDigits());
                }
            }
        }

        PaymentMessage paymentMessage = paymentMessageBuilder.build();

        orderPaidWebHookAction(paymentMessage);

        sendEventService.sendEventMessage(TopicName.PAYMENTS, paymentMessage);
        log.debug("triggered event PAYMENT_PAID for paymentId: " + payment.getPaymentId());
    }

    protected void orderPaidWebHookAction(PaymentMessage message) {
        try {
            sendNotificationService.sendPaymentMessageNotification(message);
        } catch (Exception e) {
            log.error("webhookAction: failed action after receiving event, eventType: " + message.getEventType(), e);
        }
    }

    public void triggerPaymentFailedEvent(Payment payment) {
        PaymentMessage paymentMessage = PaymentMessage.builder()
                .eventType(EventType.PAYMENT_FAILED)
                .namespace(payment.getNamespace())
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                //.paymentPaidTimestamp(payment.getTimestamp())
                .orderType(payment.getPaymentType())
                .build();
        sendEventService.sendEventMessage(TopicName.PAYMENTS, paymentMessage);
        log.debug("triggered event PAYMENT_FAILED for paymentId: " + payment.getPaymentId());
    }


    public void triggerPaymentCardRenewalEvent(Payment payment, PaymentCardInfoDto cardInfo) {
        String encryptedCardToken = EncryptorUtil.encryptValue(cardInfo.getCardToken(), cardTokenEncryptionPassword);
        PaymentMessage paymentMessage = PaymentMessage.builder()
                .eventType(EventType.SUBSCRIPTION_CARD_RENEWAL_CREATED)
                .namespace(payment.getNamespace())
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .paymentPaidTimestamp(payment.getTimestamp())
                .orderType(OrderType.SUBSCRIPTION)
                .encryptedCardToken(encryptedCardToken) // Encrypted, from dto
                .cardTokenExpMonth(cardInfo.getExpMonth())
                .cardTokenExpYear(cardInfo.getExpYear())
                .cardLastFourDigits(cardInfo.getCardLastFourDigits())
                .build();
        sendEventService.sendEventMessage(TopicName.PAYMENTS, paymentMessage);
        log.debug("triggered event {} for paymentId: {}", EventType.SUBSCRIPTION_CARD_RENEWAL_CREATED, payment.getPaymentId());
    }


    public Payment findByIdValidateByUser(String namespace, String orderId, String userId) {
        Payment payment = getPaymentForOrder(orderId);
        if (payment == null) {
            log.debug("no returnable payment, orderId: " + orderId);
            Error error = new Error("payment-not-found-from-backend", "paid or payable payment with order id [" + orderId + "] not found from backend");
            throw new CommonApiException(HttpStatus.NOT_FOUND, error);
        }

        String paymentUserId = payment.getUserId();
        if (!paymentUserId.equals(userId)) {
            log.error("unauthorized attempt to load payment, userId does not match");
            Error error = new Error("payment-not-found-from-backend", "payment with order id [" + orderId + "] and user id ["+ userId +"] not found from backend");
            throw new CommonApiException(HttpStatus.NOT_FOUND, error);
        }

        return payment;
    }

    public void updatePaymentStatus(String paymentId, PaymentReturnDto paymentReturnDto) {
        updatePaymentStatus(paymentId, paymentReturnDto, null);
    }

    public void updatePaymentStatus(String paymentId, PaymentReturnDto paymentReturnDto, PaymentCardInfoDto card) {
        Payment payment = getPayment(paymentId);

        if (paymentReturnDto.isValid()) {
            if (paymentReturnDto.isPaymentPaid()) {
                // if not already paid earlier
                if (!PaymentStatus.PAID_ONLINE.equals(payment.getStatus())) {
                    setPaymentStatus(paymentId, PaymentStatus.PAID_ONLINE);
                    triggerPaymentPaidEvent(payment, card);
                } else {
                    log.debug("not triggering events, payment paid earlier, paymentId: " + paymentId);
                }
            } else if (!paymentReturnDto.isPaymentPaid() && !paymentReturnDto.isCanRetry() && !Objects.equals(payment.getPaymentType(), PaymentType.CARD_RENEWAL)) {
                // if not already cancelled earlier
                if (!PaymentStatus.CANCELLED.equals(payment.getStatus())) {
                    setPaymentStatus(paymentId, PaymentStatus.CANCELLED);
                    triggerPaymentFailedEvent(payment);
                } else {
                    log.debug("not triggering events, payment cancelled earlier, paymentId: " + paymentId);
                }
            } else if (!paymentReturnDto.isPaymentPaid() && paymentReturnDto.isAuthorized() && Objects.equals(payment.getPaymentType(), PaymentType.CARD_RENEWAL)){
                // if not already authorized earlier
                if (!PaymentStatus.AUTHORIZED.equals(payment.getStatus())) {
                    setPaymentStatus(paymentId, PaymentStatus.AUTHORIZED);

                    PaymentCardInfoDto cardInfo = getPaymentCardToken(paymentId);
                    triggerPaymentCardRenewalEvent(payment, cardInfo);
                } else {
                    log.debug("not triggering events, payment authorized earlier, paymentId: " + paymentId);
                }
            } else {
                log.debug("not triggering events, payment not paid but can be retried, paymentId: " + paymentId);
            }
        }
    }

    public ChargeCardTokenRequestDataDto createChargeCardTokenRequestDataDto(OrderMessage message, String paymentId) {
        return new ChargeCardTokenRequestDataDto(
                message.getNamespace(),
                message.getOrderId(),
                message.getOrderItemId(),
                message.getProductName(),
                message.getProductQuantity(),
                message.getPriceTotal(),
                message.getPriceNet(),
                message.getPriceVat(),
                message.getVatPercentage(),
                message.getCardToken(),
                paymentId
        );
    }

    public OrderItemDto getCardRenewalOrderItem(String orderId){
        int chargeAmount = 1;
        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setOrderItemId(UUIDGenerator.generateType4UUID().toString());
        orderItemDto.setOrderId(orderId);
        orderItemDto.setProductId(UUIDGenerator.generateType4UUID().toString());
        orderItemDto.setProductName("card_renewal");
        orderItemDto.setQuantity(chargeAmount);
        orderItemDto.setUnit("pcs");
        orderItemDto.setRowPriceNet(new BigDecimal(chargeAmount));
        orderItemDto.setRowPriceVat(new BigDecimal(0));
        orderItemDto.setRowPriceTotal(new BigDecimal(chargeAmount));
        orderItemDto.setVatPercentage("0");
        orderItemDto.setPriceNet(new BigDecimal(chargeAmount));
        orderItemDto.setPriceVat(new BigDecimal(0));
        orderItemDto.setPriceGross(new BigDecimal(chargeAmount));
        return orderItemDto;
    }

}

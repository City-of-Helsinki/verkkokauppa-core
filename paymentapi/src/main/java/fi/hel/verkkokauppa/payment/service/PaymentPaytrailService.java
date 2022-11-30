package fi.hel.verkkokauppa.payment.service;

import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.util.StringUtils;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderItemDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentMethodDto;
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
import org.helsinki.paytrail.model.paymentmethods.PaytrailPaymentMethod;
import org.helsinki.paytrail.model.payments.PaytrailPaymentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;


@Service
@Slf4j
public class PaymentPaytrailService {

    private final PaytrailPaymentClient paytrailPaymentClient;
    private final PaytrailPaymentContextBuilder paymentContextBuilder;
    private final PaymentRepository paymentRepository;
    private final PayerRepository payerRepository;
    private final PaymentItemRepository paymentItemRepository;

    private final PaytrailPaymentProviderListMapper paytrailPaymentProviderListMapper;

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

    private Payment createPayment(PaytrailPaymentContext context, GetPaymentRequestDataDto dto, String type, String paymentId, PaytrailPaymentResponse paymentResponse) {
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
        payment.setPaytrailTransactionId(paymentResponse.getTransactionId());
        List<PaytrailPaymentProviderModel> providers = paytrailPaymentProviderListMapper.fromDto(paymentResponse.getProviders());
        payment.setPaytrailProviders(providers);
        payment.setPaymentGateway(PaymentGatewayEnum.PAYTRAIL);

        createPayer(order, paymentId);

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
}

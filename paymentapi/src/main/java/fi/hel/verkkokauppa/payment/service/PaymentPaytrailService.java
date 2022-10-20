package fi.hel.verkkokauppa.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderItemDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentMethodDto;
import fi.hel.verkkokauppa.payment.constant.GatewayEnum;
import fi.hel.verkkokauppa.payment.logic.builder.PaytrailPaymentContextBuilder;
import fi.hel.verkkokauppa.payment.logic.context.PaymentContext;
import fi.hel.verkkokauppa.payment.logic.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.model.Payer;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.PaymentItem;
import fi.hel.verkkokauppa.payment.model.PaymentStatus;
import fi.hel.verkkokauppa.payment.paytrail.factory.PaytrailAuthClientFactory;
import fi.hel.verkkokauppa.payment.repository.PayerRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentItemRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentRepository;
import fi.hel.verkkokauppa.payment.util.PaymentUtil;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.PaytrailClient;
import org.helsinki.paytrail.mapper.PaytrailPaymentMethodsResponseMapper;
import org.helsinki.paytrail.request.paymentmethods.PaytrailPaymentMethodsRequest;
import org.helsinki.paytrail.response.paymentmethods.PaytrailPaymentMethodsResponse;
import org.helsinki.vismapay.request.payment.ChargeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class PaymentPaytrailService {

    private final PaytrailAuthClientFactory paytrailClientFactory;
    private final CommonServiceConfigurationClient commonServiceConfigurationClient;
    private final PaytrailPaymentMethodsResponseMapper paymentMethodsResponseMapper;
    private final PaytrailPaymentContextBuilder paymentContextBuilder;
    private final PaymentRepository paymentRepository;
    private final PayerRepository payerRepository;
    private final PaymentItemRepository paymentItemRepository;

    @Autowired
    PaymentPaytrailService(
            PaytrailAuthClientFactory paytrailClientFactory,
            CommonServiceConfigurationClient commonServiceConfigurationClient,
            ObjectMapper mapper,
            PaytrailPaymentContextBuilder paymentContextBuilder,
            PaymentRepository paymentRepository,
            PayerRepository payerRepository,
            PaymentItemRepository paymentItemRepository
    ) {
        this.paytrailClientFactory = paytrailClientFactory;
        this.commonServiceConfigurationClient = commonServiceConfigurationClient;
        this.paymentMethodsResponseMapper = new PaytrailPaymentMethodsResponseMapper(mapper);
        this.paymentContextBuilder = paymentContextBuilder;
        this.paymentRepository = paymentRepository;
        this.payerRepository = payerRepository;
        this.paymentItemRepository = paymentItemRepository;
    }

    public PaymentMethodDto[] getOnlinePaymentMethodList(String merchantId, String namespace, String currency) {
        if (merchantId != null && !merchantId.isEmpty()) {
            String shopId = commonServiceConfigurationClient.getMerchantConfigurationValue(merchantId, namespace, ServiceConfigurationKeys.MERCHANT_SHOP_ID);
            if (shopId != null && !shopId.isEmpty()) {
                PaytrailClient paytrailClient = paytrailClientFactory.getClient(shopId);
                try {
                    PaytrailPaymentMethodsRequest.PaymentMethodsPayload payload = new PaytrailPaymentMethodsRequest.PaymentMethodsPayload();
                    PaytrailPaymentMethodsRequest request = new PaytrailPaymentMethodsRequest(payload);
                    CompletableFuture<PaytrailPaymentMethodsResponse> response = paytrailClient.sendRequest(request);
                    PaytrailPaymentMethodsResponse methodsResponse = paymentMethodsResponseMapper.to(response.get());

                    return methodsResponse.getPaymentMethods().stream().map(paymentMethod -> new PaymentMethodDto(
                            paymentMethod.getName(),
                            paymentMethod.getId(),
                            paymentMethod.getGroup(),
                            paymentMethod.getIcon(),
                            GatewayEnum.ONLINE_PAYTRAIL
                    )).toArray(PaymentMethodDto[]::new);

                } catch (ExecutionException | InterruptedException | RuntimeException e) {
                    log.warn("getting online paytrail payment methods failed, currency: " + currency, e);
                    return new PaymentMethodDto[0];
                }
            } else {
                log.debug("shopId is not cannot be null or empty!");
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

        PaytrailPaymentContext context = paymentContextBuilder.buildFor(namespace, merchantId);

        /* NOT NEEDED in paytrail flow
        ChargeRequest.PaymentTokenPayload tokenRequestPayload = paymentTokenPayloadBuilder.buildFor(dto, context);
        log.debug("tokenRequestPayload: " + tokenRequestPayload);

        String paymentId = tokenRequestPayload.getOrderNumber();
        String token = paymentTokenFetcher.getToken(tokenRequestPayload);   // CREATE VISMA PAYMENT
        */

        String paymentId = PaymentUtil.generatePaymentOrderNumber(order.getOrderId());


        Payment payment = createPayment(dto, paymentType, paymentId);
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

    private Payment createPayment(GetPaymentRequestDataDto dto, String type, String paymentId) {
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

}

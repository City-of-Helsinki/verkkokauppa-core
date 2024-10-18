package fi.hel.verkkokauppa.payment.service;

import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.util.StringUtils;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderItemDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentReturnDto;
import fi.hel.verkkokauppa.payment.constant.PaymentGatewayEnum;
import fi.hel.verkkokauppa.payment.model.Payer;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.PaymentItem;
import fi.hel.verkkokauppa.payment.model.PaymentStatus;
import fi.hel.verkkokauppa.payment.repository.PayerRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentItemRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentRepository;
import fi.hel.verkkokauppa.payment.util.PaymentUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;

@Service
@Slf4j
public class FreePaymentService {
    private final PaymentRepository paymentRepository;
    private final PayerRepository payerRepository;
    private final PaymentItemRepository paymentItemRepository;
    private Environment env;

    @Autowired
    FreePaymentService(PaymentRepository paymentRepository, PayerRepository payerRepository, PaymentItemRepository paymentItemRepository, Environment env) {
        this.paymentRepository = paymentRepository;
        this.payerRepository = payerRepository;
        this.paymentItemRepository = paymentItemRepository;
        this.env = env;
    }

    public Payment createFromOrder(GetPaymentRequestDataDto dto) {
        OrderDto order = dto.getOrder().getOrder();
        List<OrderItemDto> items = dto.getOrder().getItems();

        if (items.isEmpty()) {
            throw new IllegalArgumentException("Items cannot be empty.");
        }

        String namespace = order.getNamespace();
        String orderId = order.getOrderId();
        String orderStatus = order.getStatus();
        String userId = order.getUser();

        isValidOrderStatusToCreatePayment(orderId, orderStatus);
        isValidUserToCreatePayment(orderId, userId);

        boolean isRecurringOrder = order.getType().equals(OrderType.SUBSCRIPTION);
        String paymentType = isRecurringOrder ? OrderType.SUBSCRIPTION : OrderType.ORDER;

        String paymentId = PaymentUtil.generatePaymentOrderNumber(order.getOrderId());

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setNamespace(order.getNamespace());
        payment.setOrderId(order.getOrderId());
        payment.setUserId(order.getUser());
        payment.setPaymentMethod(dto.getPaymentMethod());
        payment.setPaymentMethodLabel(dto.getPaymentMethodLabel());
        payment.setTimestamp(sdf.format(timestamp));
        payment.setAdditionalInfo("{\"payment_method\": " + dto.getPaymentMethod() + "}");
        payment.setPaymentType(paymentType);
        payment.setStatus(PaymentStatus.FREE);
        payment.setTotalExclTax(new BigDecimal(order.getPriceNet()));
        payment.setTaxAmount(new BigDecimal(order.getPriceVat()));
        payment.setTotal(new BigDecimal(order.getPriceTotal()));
        payment.setPaymentGateway(PaymentGatewayEnum.FREE);

        createPayer(order, paymentId);

        for (OrderItemDto item : items) {
            createPaymentItem(item, paymentId, order.getOrderId());
        }

        log.debug("creating free payment for namespace: " + namespace + " with paymentId: " + paymentId);
        paymentRepository.save(payment);

        return payment;
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

    private void isValidUserToCreatePayment(String orderId, String userId) {
        if (userId == null || userId.isEmpty()) {
            log.warn("creating payment without user rejected, orderId: " + orderId);
            throw new CommonApiException(
                    HttpStatus.FORBIDDEN,
                    new Error("rejected-creating-payment-for-order-without-user", "rejected creating payment for order without user, order id [" + orderId + "]")
            );
        }
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

    public String getPaymentUrl(Payment payment) {
        return env.getRequiredProperty("free_payment_url") + "?orderId=" + payment.getOrderId() + "&user=" + payment.getUserId();
    }

    public boolean validatePayment(Payment payment) {
        boolean isValid = false;
        if( payment != null ){
            BigDecimal zeroValue = BigDecimal.ZERO;
            boolean paymentValidation = payment.getTotal().compareTo(zeroValue) == 0;
            boolean gatewayValidation = payment.getPaymentGateway().equals(PaymentGatewayEnum.FREE);
            // check that total is zero and gateway is free
            isValid = gatewayValidation && paymentValidation;
        }
        return isValid;
    }

    public PaymentReturnDto createPaymentReturnDto(boolean isValid) {
        boolean isPaymentPaid = true;
        boolean canRetry = false;
        boolean isAuthorized = false;

        return new PaymentReturnDto(isValid, isPaymentPaid, canRetry, isAuthorized);
    }

    private boolean isSuccessful(String status) {
        return org.helsinki.paytrail.constants.PaymentStatus.OK.getStatus().equals(status);
    }
}

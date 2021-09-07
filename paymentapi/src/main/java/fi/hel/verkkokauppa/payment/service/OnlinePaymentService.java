package fi.hel.verkkokauppa.payment.service;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderItemDto;
import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;
import fi.hel.verkkokauppa.payment.logic.PaymentContext;
import fi.hel.verkkokauppa.payment.logic.PaymentContextBuilder;
import fi.hel.verkkokauppa.payment.logic.PaymentTokenPayloadBuilder;
import fi.hel.verkkokauppa.payment.logic.TokenFetcher;
import fi.hel.verkkokauppa.payment.model.Payer;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.PaymentItem;
import fi.hel.verkkokauppa.payment.model.PaymentStatus;
import fi.hel.verkkokauppa.payment.repository.PayerRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentItemRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentRepository;
import org.helsinki.vismapay.VismaPayClient;
import org.helsinki.vismapay.request.payment.ChargeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
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
    private PaymentTokenPayloadBuilder payloadBuilder;

    @Autowired
    private TokenFetcher tokenFetcher;

    @Autowired
    private PaymentContextBuilder paymentContextBuilder;

    public Payment getPaymentRequestData(GetPaymentRequestDataDto dto) {
        try {
            OrderDto order = dto.getOrder().getOrder();
            String namespace = order.getNamespace();
            String orderId = order.getOrderId();
            String orderStatus = order.getStatus();
            String userId = order.getUser();

            // check order status, can only create payment for confirmed orders
            if (!"confirmed".equals(orderStatus)) {
                log.warn("creating payment for unconfirmed order rejected, orderId: " + orderId);
                throw new CommonApiException(
                        HttpStatus.FORBIDDEN,
                        new Error("rejected-creating-payment-for-unconfirmed-order", "rejected creating payment for unconfirmed order, order id [" + orderId + "]")
                );
            }

            if (userId == null || userId.isEmpty()) {
                log.warn("creating payment without user rejected, orderId: " + orderId);
                throw new CommonApiException(
                        HttpStatus.FORBIDDEN,
                        new Error("rejected-creating-payment-for-order-without-user", "rejected creating payment for order without user, order id [" + orderId + "]")
                );
            }

            boolean isRecurringOrder = order.getType().equals("subscription");
            String paymentType = isRecurringOrder ? "subscription" : "order";

            PaymentContext context = paymentContextBuilder.buildFor(namespace);

            ChargeRequest.PaymentTokenPayload tokenRequestPayload = payloadBuilder.buildFor(dto, context);
            log.debug("tokenRequestPayload: " + tokenRequestPayload);

            String paymentId = tokenRequestPayload.getOrderNumber();
            String token = tokenFetcher.getToken(tokenRequestPayload);

            Payment payment = createPayment(dto, paymentType, token, paymentId);
            if (payment.getPaymentId() == null) {
                throw new RuntimeException("Didn't manage to create payment.");
            }

            return payment;
        } catch (Exception e) {
            log.error("creating payment or chargerequest failed", e);
            return null; // TODO: return failure url
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

    public Payment getPaymentForOrder(String orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(orderId);

        Payment paidPayment = selectPaidPayment(payments);
        Payment payablePayment = selectPayablePayment(payments);

        if (paidPayment != null)
            return paidPayment;
        else if (payablePayment != null)
            return payablePayment;
        else {
            log.debug("no returnable payment, orderId: " + orderId);
            Error error = new Error("payment-not-found-from-backend", "paid or payable payment with order id [" + orderId + "] not found from backend");
            throw new CommonApiException(HttpStatus.NOT_FOUND, error);
        }
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

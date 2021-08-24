package fi.hel.verkkokauppa.payment.service;

import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderItemDto;
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
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
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
        // TODO: should check order status - if wrong status, return failure url

        try {
            String namespace = dto.getOrder().getOrder().getNamespace();

            boolean isRecurringOrder = dto.getOrder().getOrder().getType().equals("subscription");
            String paymentType = isRecurringOrder ? "subscription" : "order"; // TODO: ok?

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

    public String getPaymentUrl(String namespace, String orderId) {
        Payment payment = getPaymentForOrder(namespace, orderId);
        return getPaymentUrl(payment.getToken());
    }

    public String getPaymentStatus(String namespace, String orderId) {
        Payment payment = getPaymentForOrder(namespace, orderId);
        return payment.getStatus();
    }

    public void setPaymentStatus(String paymentId, String status) {
        Payment payment = getPayment(paymentId);
        payment.setStatus(status);
        paymentRepository.save(payment);
    }

    public Payment getPaymentForOrder(String namespace, String orderId) {
        List<Payment> payments = paymentRepository.findByNamespaceAndOrderId(namespace, orderId);

        if (payments.isEmpty()) {
            throw new IllegalArgumentException("Payment not found.");
        }

        return payments.get(payments.size()-1);
    }

    public Payment getPaymentForOrder(String orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(orderId);

        if (payments.isEmpty()) {
            throw new IllegalArgumentException("Payment not found.");
        }

        return payments.get(payments.size()-1);
    }

    public Payment getPayment(String paymentId) {
        Optional<Payment> payment = paymentRepository.findById(paymentId);

        if (false == payment.isPresent()) {
            log.warn("payment not found, paymentId: " + paymentId);
            throw new IllegalArgumentException("Payment not found.");
        }
        return payment.get();
    }

    private Payment createPayment(GetPaymentRequestDataDto dto, String type, String token, String paymentId) {
        OrderDto order = dto.getOrder().getOrder();
        List<OrderItemDto> items = dto.getOrder().getItems();

        if (items.isEmpty()) {
            throw new IllegalArgumentException("Items cannot be empty.");
        }

        String namespace = order.getNamespace();

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setNamespace(order.getNamespace());
        payment.setOrderId(order.getOrderId());
        payment.setPaymentMethod(dto.getPaymentMethod());
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

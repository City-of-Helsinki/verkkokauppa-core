package fi.hel.verkkokauppa.payment.service;

import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderItemDto;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public String getPaymentRequestData(GetPaymentRequestDataDto dto) {
        // TODO: should check order status
        // TODO: if wrong status, return failure url (where to get it from?)

        Payment payment = createPayment(dto);
        // TODO: if creating payment fails, return failure url

        try {
            String token = tokenFetcher.getToken(payloadBuilder.buildFor(dto));

            // TODO: Kortin token tulee tallentaa jos subscription, mistä tiedetään?
            return VismaPayClient.API_URL + "/token/" + token;
        } catch (RuntimeException e) {
            return null; // TODO: return failure url
        }
    }

    // TODO: transaction?
    private Payment createPayment(GetPaymentRequestDataDto dto) {
        OrderDto order = dto.getOrder().getOrder();
        List<OrderItemDto> items = dto.getOrder().getItems();

        if (items.isEmpty()) {
            throw new IllegalArgumentException("Items cannot be empty.");
        }

        String namespace = order.getNamespace();
        String paymentId = UUIDGenerator.generateType3UUIDString(namespace, order.getOrderId());

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setNamespace(order.getNamespace());
        payment.setOrderId(order.getOrderId());
        // TODO: additional info => how?
        // TODO: set payment method?

        calculateTotals(payment, items);

        payment.setStatus(PaymentStatus.CREATED);

        for (OrderItemDto item : items) {
            createPaymentItem(item, paymentId, order.getOrderId());
        }
        createPayer(order);

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
        item.setRowPriceTotal(itemDto.getRowPriceTotal());
        item.setRowPriceVat(itemDto.getRowPriceVat());
        // TODO: something else?

        paymentItemRepository.save(item);
    }

    private void createPayer(OrderDto orderDto) {
        Payer payer = new Payer();
        payer.setFirstName(orderDto.getCustomerFirstName());
        payer.setLastName(orderDto.getCustomerLastName());
        payer.setEmail(orderDto.getCustomerEmail());

        payerRepository.save(payer);
    }

    private void calculateTotals(Payment payment, List<OrderItemDto> items) {
        for (OrderItemDto item : items) {
            // TODO: somehow calculate the following:
            /*
            BigDecimal totalExclTax;
            BigDecimal total;
            BigDecimal taxPercent;
            BigDecimal taxAmount;
         */
        }
    }
}

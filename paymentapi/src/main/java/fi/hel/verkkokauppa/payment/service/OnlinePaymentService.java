package fi.hel.verkkokauppa.payment.service;

import org.json.JSONObject;
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
import org.springframework.web.reactive.function.client.WebClient;


import java.math.BigDecimal;
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

    @Autowired
    private ServiceConfigurationClient serviceConfigurationClient;


    public String getPaymentRequestData(GetPaymentRequestDataDto dto) {
        // TODO: should check order status - if wrong status, return failure url

        boolean isRecurringOrder = dto.getOrder().getOrder().getType().equals("subscription");
        String paymentType = isRecurringOrder ? "subscription" : "order"; // TODO: ok?
        // get common payment configuration from configuration api
        WebClient client = serviceConfigurationClient.getClient();
        String namespace = dto.getOrder().getOrder().getNamespace();
        JSONObject namespaceServiceConfiguration = serviceConfigurationClient.getAllServiceConfiguration(client, namespace);

        // refer to ServiceConfigurationKeys
        String payment_return_url = (String) namespaceServiceConfiguration.get("payment_return_url");
        log.debug("namespace: " + namespace + " payment_return_url: " + payment_return_url);

        // TODO use the common payment configuration values

        try {
            String token = tokenFetcher.getToken(payloadBuilder.buildFor(dto));

            Payment payment = createPayment(dto, paymentType, isRecurringOrder ? token : null);
            if (payment.getPaymentId() == null) {
                throw new RuntimeException("Didn't manage to create payment.");
            }

            return VismaPayClient.API_URL + "/token/" + token;
        } catch (Exception e) {
            return null; // TODO: return failure url
        }
    }

    private Payment createPayment(GetPaymentRequestDataDto dto, String type, String token) {
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
        payment.setAdditionalInfo("{\"payment_method\": " + dto.getPaymentMethod() + "}");
        payment.setPaymentType(type);
        payment.setStatus(PaymentStatus.CREATED);
        payment.setToken(token); // TODO: ok?

        calculateTotals(payment, items);
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
        item.setRowPriceTotal(itemDto.getRowPriceTotal());
        item.setRowPriceVat(itemDto.getRowPriceVat());

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
        BigDecimal totalExclTax = BigDecimal.valueOf(0);
        BigDecimal total = BigDecimal.valueOf(0);
        BigDecimal taxAmount = BigDecimal.valueOf(0);

        for (OrderItemDto item : items) {
            totalExclTax = totalExclTax.add(item.getRowPriceNet());
            total = total.add(item.getRowPriceTotal());
            taxAmount = taxAmount.add(item.getRowPriceVat());
        }

        payment.setTotal(total);
        payment.setTotalExclTax(totalExclTax);
        payment.setTaxAmount(taxAmount);
        payment.setTaxPercent(taxAmount.divide(totalExclTax)); // TODO: ok?
    }
}

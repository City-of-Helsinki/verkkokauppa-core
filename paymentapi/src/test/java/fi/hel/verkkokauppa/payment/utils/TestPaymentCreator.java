package fi.hel.verkkokauppa.payment.utils;

import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderItemDto;
import fi.hel.verkkokauppa.payment.model.Payer;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.PaymentItem;
import fi.hel.verkkokauppa.payment.model.PaymentStatus;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class TestPaymentCreator {
    public static Payment getDummyPayment(String orderId, String userId, String namespace) {
        Payment payment = new Payment();
        payment.setPaymentId("payment_1");
        payment.setNamespace(namespace);
        payment.setOrderId(orderId);
        payment.setUserId(userId);
        payment.setStatus("payment_paid_online");
        payment.setPaymentMethod("nordea");
        payment.setPaymentType("order");
        payment.setTotalExclTax(new BigDecimal("64.52"));
        payment.setTotal(new BigDecimal("80"));
        payment.setTaxAmount(new BigDecimal("15.48"));
        payment.setDescription(null);
        payment.setAdditionalInfo("{\"payment_method\": nordea}");
        payment.setToken("032b298c0eb904175bf228a47f36f34ec1f11eebcc8506e5cd1f279a805914b9");
        payment.setTimestamp("20211025-135602");
        payment.setPaymentMethodLabel("Nordea");
        return payment;
    }

    public static Payment createDummyPaymentFromGetPaymentRequest(GetPaymentRequestDataDto dto, String type, String paymentId) {
        OrderDto order = dto.getOrder().getOrder();

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
        payment.setPaymentType(type);
        payment.setStatus(PaymentStatus.CREATED);
        payment.setTotalExclTax(new BigDecimal(order.getPriceNet()));
        payment.setTaxAmount(new BigDecimal(order.getPriceVat()));
        payment.setTotal(new BigDecimal(order.getPriceTotal()));
        payment.setPaytrailTransactionId("2e60d1e0-0f5a-4f1b-ad95-d9b0fc00202a");

        return payment;
    }

    public static PaymentItem createDummyPaymentItem(String paymentId, OrderItemDto itemDto) {
        PaymentItem item = new PaymentItem();
        item.setPaymentId(paymentId);
        item.setOrderId(itemDto.getOrderId());
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
        return item;
    }

    public static Payer createDummyPayer(String paymentId, OrderDto orderDto) {
        Payer payer = new Payer();
        payer.setPaymentId(paymentId);
        payer.setFirstName(orderDto.getCustomerFirstName());
        payer.setLastName(orderDto.getCustomerLastName());
        payer.setEmail(orderDto.getCustomerEmail());
        return payer;
    }
}

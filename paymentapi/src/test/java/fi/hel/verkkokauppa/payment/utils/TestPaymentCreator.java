package fi.hel.verkkokauppa.payment.utils;

import fi.hel.verkkokauppa.payment.model.Payment;

import java.math.BigDecimal;

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
}

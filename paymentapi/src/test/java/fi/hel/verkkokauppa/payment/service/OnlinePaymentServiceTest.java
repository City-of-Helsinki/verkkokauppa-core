package fi.hel.verkkokauppa.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.payment.model.Payment;
import org.junit.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OnlinePaymentServiceTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void selectPaidPayment() throws JsonProcessingException {
        OnlinePaymentService onlinePaymentService = new OnlinePaymentService();
        List<Payment> payments = List.of(objectMapper.readValue("[\n" +
                "  {\n" +
                "    \"paymentId\": \"dummy_order_id_at_20240115-182135\",\n" +
                "    \"namespace\": \"tilanvaraus\",\n" +
                "    \"orderId\": \"dummy_order_id\",\n" +
                "    \"userId\": \"dummy_user\",\n" +
                "    \"status\": \"payment_created\",\n" +
                "    \"paymentMethod\": \"spankki\",\n" +
                "    \"paymentType\": \"order\",\n" +
                "    \"totalExclTax\": 142.5,\n" +
                "    \"total\": 176.7,\n" +
                "    \"taxAmount\": 34.2,\n" +
                "    \"additionalInfo\": \"{\\\"payment_method\\\": spankki}\",\n" +
                "    \"timestamp\": \"20240115-182135\",\n" +
                "    \"paymentMethodLabel\": \"S-Pankki\",\n" +
                "    \"paytrailTransactionId\": \"transactionId\",\n" +
                "    \"shopInShopPayment\": false\n" +
                "  },\n" +
                "  {\n" +
                "    \"paymentId\": \"dummy_order_id_at_20240115-181549\",\n" +
                "    \"namespace\": \"tilanvaraus\",\n" +
                "    \"orderId\": \"dummy_order_id\",\n" +
                "    \"userId\": \"dummy_user\",\n" +
                "    \"status\": \"payment_created\",\n" +
                "    \"paymentMethod\": \"spankki\",\n" +
                "    \"paymentType\": \"order\",\n" +
                "    \"totalExclTax\": 142.5,\n" +
                "    \"total\": 176.7,\n" +
                "    \"taxAmount\": 34.2,\n" +
                "    \"additionalInfo\": \"{\\\"payment_method\\\": spankki}\",\n" +
                "    \"timestamp\": \"20240115-181549\",\n" +
                "    \"paymentMethodLabel\": \"S-Pankki\",\n" +
                "    \"paytrailTransactionId\": \"transactionId\",\n" +
                "    \"shopInShopPayment\": false\n" +
                "  },\n" +
                "  {\n" +
                "    \"paymentId\": \"dummy_order_id_at_20240115-182125\",\n" +
                "    \"namespace\": \"tilanvaraus\",\n" +
                "    \"orderId\": \"dummy_order_id\",\n" +
                "    \"userId\": \"dummy_user\",\n" +
                "    \"status\": \"payment_paid_online\",\n" +
                "    \"paymentMethod\": \"spankki\",\n" +
                "    \"paymentType\": \"order\",\n" +
                "    \"totalExclTax\": 142.5,\n" +
                "    \"total\": 176.7,\n" +
                "    \"taxAmount\": 34.2,\n" +
                "    \"additionalInfo\": \"{\\\"payment_method\\\": spankki}\",\n" +
                "    \"timestamp\": \"20240115-182125\",\n" +
                "    \"paymentMethodLabel\": \"S-Pankki\",\n" +
                "    \"paytrailTransactionId\": \"transactionId\",\n" +
                "    \"shopInShopPayment\": false\n" +
                "  }\n" +
                "]", Payment[].class));
        Payment paidPayment = onlinePaymentService.selectPaidPayment(payments);
        assertEquals("dummy_order_id_at_20240115-182125", paidPayment.getPaymentId());
        assertEquals("payment_paid_online", paidPayment.getStatus());
        assertEquals("transactionId", paidPayment.getPaytrailTransactionId());
        assertEquals("S-Pankki", paidPayment.getPaymentMethodLabel());
        assertEquals("spankki", paidPayment.getPaymentMethod());
        assertEquals("order", paidPayment.getPaymentType());
    }
}
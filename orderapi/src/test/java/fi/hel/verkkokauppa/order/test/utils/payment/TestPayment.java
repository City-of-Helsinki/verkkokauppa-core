package fi.hel.verkkokauppa.order.test.utils.payment;

import lombok.Data;

import java.math.BigDecimal;

@Data()
public class TestPayment {
    private String paymentId;
    private String namespace;
    private String orderId;
    private String userId;
    private String status;
    private String paymentMethod;
    private String paymentType;
    private BigDecimal totalExclTax;
    private BigDecimal total;
    private BigDecimal taxAmount;
    private String description;
    private String additionalInfo;
    private String token;
    private String timestamp;
    private String paymentMethodLabel;

}
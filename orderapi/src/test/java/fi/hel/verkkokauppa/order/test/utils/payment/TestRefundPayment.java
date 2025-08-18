package fi.hel.verkkokauppa.order.test.utils.payment;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data()
public class TestRefundPayment {
    private String namespace;
    private String orderId;
    private String userId;
    private String status;
    private String paymentType;
    private BigDecimal totalExclTax;
    private BigDecimal total;
    private BigDecimal taxAmount;
    private String description;
    private String additionalInfo;
    private String token;
    private String timestamp;

    // refundId (Refund model id) + timestamp (Like creating paymentId)
    private String refundPaymentId;

    // Paytrail gives this when we create refund for payment
    private String refundTransactionId;

    private String refundMethod;

    private String refundGateway;

    private String refundId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;


}
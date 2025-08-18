package fi.hel.verkkokauppa.common.payment;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefundPaymentCommonDto {
    // refundId (Refund model id) + timestamp (Like creating paymentId)
    private String refundPaymentId;

    // Paytrail gives this when we create refund for payment
    private String refundTransactionId;

    private String namespace;

    private String orderId;

    private String userId;

    // RefundPaymentStatus.CREATED
    private String status;

    // payment.paymentMethod
    private String refundMethod;

    // RefundGateway.PAYTRAIL

    private String refundGateway;

    // refund.getPriceNet()

    private BigDecimal totalExclTax;

    // refund.getPriceTotal()

    private BigDecimal total;

    // refund.getRefundId()

    private String refundId;

    // refund.getPriceVat()
    private BigDecimal taxAmount;

    private String timestamp;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    LocalDateTime paidAt; // Timestamp when the transaction was refunded

    String paymentProviderStatus;
}

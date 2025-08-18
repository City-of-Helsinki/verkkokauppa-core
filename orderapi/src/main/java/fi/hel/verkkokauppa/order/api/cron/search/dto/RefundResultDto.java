package fi.hel.verkkokauppa.order.api.cron.search.dto;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefundResultDto {
    // refundId (Refund model id) + timestamp (Like creating paymentId)
    private String refundPaymentId;

    private String refundTransactionId;
    private String paytrailMerchantId;

//    private String merchantId;

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

}

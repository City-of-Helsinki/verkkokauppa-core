package fi.hel.verkkokauppa.payment.model.refund;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Document(indexName = "refund_payments")
public class RefundPayment {

	// refundId (Refund model id) + timestamp (Like creating paymentId)
	@Id
	private String refundPaymentId;

	// Paytrail gives this when we create refund for payment
	@Field(type = FieldType.Text)
	private String refundTransactionId;

	@Field(type = FieldType.Keyword)
	private String namespace;

	@Field(type = FieldType.Text)
	private String orderId;

	@Field(type = FieldType.Text)
	private String userId;

	// RefundPaymentStatus.CREATED
	@Field(type = FieldType.Text)
	private String status;

	// payment.paymentMethod
	@Field(type = FieldType.Text)
	private String refundMethod;

	// RefundGateway.PAYTRAIL
	@Field(type = FieldType.Text)
	private String refundGateway;

	// refund.getPriceNet()
	@Field(type = FieldType.Double)
	private BigDecimal totalExclTax;

	// refund.getPriceTotal()
	@Field(type = FieldType.Double)
	private BigDecimal total;

	// refund.getRefundId()
	@Field(type = FieldType.Text)
	private String refundId;

	// refund.getPriceVat()
	@Field(type = FieldType.Double)
	private BigDecimal taxAmount;

	@Field(type = FieldType.Text)
	private String timestamp;

	@Field(type = FieldType.Date, format = DateFormat.date_optional_time)
	private LocalDateTime createdAt;

	@Field(type = FieldType.Date, format = DateFormat.date_optional_time)
	private LocalDateTime updatedAt;


	public RefundPayment() {
		this.status = RefundPaymentStatus.CREATED;
		this.createdAt = LocalDateTime.now();
	}

}

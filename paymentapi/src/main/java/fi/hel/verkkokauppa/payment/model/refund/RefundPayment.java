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
	String refundPaymentId;

	// Paytrail gives this when we create refund for payment
	String refundTransactionId;

	@Field(type = FieldType.Keyword)
	String namespace;

	@Field(type = FieldType.Text)
	String orderId;

	@Field(type = FieldType.Text)
	String userId;

	// RefundPaymentStatus.CREATED
	@Field(type = FieldType.Text)
	String status;

	// haetaan orderId:n avulla maksettu payment ja sen paymentMethod
	@Field(type = FieldType.Text)
	String refundMethod;

	// order or subscription
	@Field(type = FieldType.Text)
	String refundType;

	// RefundGateway.PAYTRAIL
	@Field(type = FieldType.Text)
	String refundGateway;

	// refund.getPriceNet()
	@Field(type = FieldType.Double)
	BigDecimal totalExclTax;

	// refund.getPriceTotal()
	@Field(type = FieldType.Double)
	BigDecimal total;

	// refund.getPriceVat()
	@Field(type = FieldType.Double)
	BigDecimal taxAmount;

	@Field(type = FieldType.Text)
	String timestamp;

	@Field(type = FieldType.Date, format = DateFormat.date_optional_time)
	LocalDateTime createdAt;

	@Field(type = FieldType.Date, format = DateFormat.date_optional_time)
	LocalDateTime updatedAt;


	public RefundPayment() {
		this.status = RefundPaymentStatus.CREATED;
		this.createdAt = LocalDateTime.now();
	}

}

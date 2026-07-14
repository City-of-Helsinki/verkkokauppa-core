package fi.hel.verkkokauppa.payment.model.refund;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Getter
@Setter
@Document(indexName = "refund_payments")
public class RefundPayment implements Persistable<String> {

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
	private Instant createdAt;

	@LastModifiedDate
	@Field(type = FieldType.Date, format = DateFormat.date_optional_time)
	private Instant updatedAt;

	@Field(type = FieldType.Date, format = DateFormat.date_time)
	Instant paidAt; // Timestamp when the transaction was refunded

	@Field(type = FieldType.Text)
	String paymentProviderStatus;

	public String getId() {
		return this.refundPaymentId;
	}

	@Override
	public boolean isNew() {
		return updatedAt == null;
	}

	public RefundPayment() {
		this.status = RefundPaymentStatus.CREATED;
		this.createdAt = LocalDateTime.now().atZone(ZoneOffset.UTC).toInstant();
	}

	public LocalDateTime getCreatedAt(){
		return LocalDateTime.ofInstant(this.createdAt, ZoneOffset.UTC);
	}

	public void setCreatedAt(LocalDateTime createdAt){
		this.createdAt = createdAt.atZone(ZoneOffset.UTC).toInstant();
	}

	public LocalDateTime getUpdatedAt() {
		return LocalDateTime.ofInstant(this.updatedAt, ZoneOffset.UTC);
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt.atZone(ZoneOffset.UTC).toInstant();;
	}

	public LocalDateTime getPaidAt() {
		return LocalDateTime.ofInstant(this.paidAt, ZoneOffset.UTC);
	}

	public void setPaidAt(LocalDateTime paidAt) {
		this.paidAt = paidAt.atZone(ZoneOffset.UTC).toInstant();;
	}

}

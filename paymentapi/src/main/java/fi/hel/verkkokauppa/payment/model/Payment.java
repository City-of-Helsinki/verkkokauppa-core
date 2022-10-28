package fi.hel.verkkokauppa.payment.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Document(indexName = "payments")
public class Payment {

	@Id
	String paymentId;

	@Field(type = FieldType.Keyword)
	String namespace;

	@Field(type = FieldType.Text)
	String orderId;

	@Field(type = FieldType.Text)
	String userId;

	@Field(type = FieldType.Text)
	String status;

	@Field(type = FieldType.Text)
	String paymentMethod;

	@Field(type = FieldType.Text)
	String paymentType; // TODO: what is this?

	@Field(type = FieldType.Double)
	BigDecimal totalExclTax;

	@Field(type = FieldType.Double)
	BigDecimal total;

	@Field(type = FieldType.Double)
	BigDecimal taxAmount;

	@Field(type = FieldType.Text)
	String description; // TODO: needed?

	@Field(type = FieldType.Text)
	String additionalInfo;

	@Field(type = FieldType.Text)
	String token;

	@Field(type = FieldType.Text)
	String timestamp;

	@Field(type = FieldType.Text)
	String paymentMethodLabel;

	@Field(type = FieldType.Text)
	String paytrailTransactionId;

	public Payment() {
		this.status = PaymentStatus.CREATED;
	}

}

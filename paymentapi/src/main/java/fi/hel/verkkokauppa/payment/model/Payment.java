package fi.hel.verkkokauppa.payment.model;

import fi.hel.verkkokauppa.payment.constant.PaymentGatewayEnum;
import fi.hel.verkkokauppa.payment.model.paytrail.payment.PaytrailPaymentProviderModel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Getter
@Setter
@Document(indexName = "payments")
public class Payment implements Persistable<String> {

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

	/**
	 * There is no way to determine in the paytrail return callback methods if a payment is shopInShop payment or not.
	 * This new field will be set on payment creation to indicate what type of paytrail flow is used for the specific payment.
	 */
	@Field(type = FieldType.Boolean)
	boolean shopInShopPayment;

	// Contains paytrail provider response from paytrail payment create request, used to make redirect to payment provider in kassa-ui
	@Field(type = FieldType.Object)
	private PaytrailPaymentProviderModel paytrailProvider;

	@Field(type = FieldType.Text)
	PaymentGatewayEnum paymentGateway;

	@CreatedDate
	@Field(type = FieldType.Date, format = DateFormat.date_time)
	LocalDateTime createdAt;

	@LastModifiedDate
	@Field(type = FieldType.Date, format = DateFormat.date_time)
	LocalDateTime updatedAt;

	@Field(type = FieldType.Date, format = DateFormat.date_time)
	LocalDateTime paidAt; // Timestamp when the transaction was paid

	public String getId() {
		return this.paymentId;
	}

	@Override
	public boolean isNew() {
		return createdAt == null;
	}

	public Payment() {
		this.status = PaymentStatus.CREATED;
	}

}

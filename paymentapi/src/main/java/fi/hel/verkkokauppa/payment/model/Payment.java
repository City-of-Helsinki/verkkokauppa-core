package fi.hel.verkkokauppa.payment.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

	public String getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getPaymentType() {
		return paymentType;
	}

	public void setPaymentType(String paymentType) {
		this.paymentType = paymentType;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public BigDecimal getTotalExclTax() {
		return totalExclTax;
	}

	public void setTotalExclTax(BigDecimal totalExclTax) {
		this.totalExclTax = totalExclTax;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	public BigDecimal getTaxAmount() {
		return taxAmount;
	}

	public void setTaxAmount(BigDecimal taxAmount) {
		this.taxAmount = taxAmount;
	}

	public String getAdditionalInfo() {
		return additionalInfo;
	}

	public void setAdditionalInfo(String additionalInfo) {
		this.additionalInfo = additionalInfo;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getPaymentMethodLabel() {
		return paymentMethodLabel;
	}

	public void setPaymentMethodLabel(String paymentMethodLabel) {
		this.paymentMethodLabel = paymentMethodLabel;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPaytrailTransactionId() {
		return paytrailTransactionId;
	}

	public void setPaytrailTransactionId(String paytrailTransactionId) {
		this.paytrailTransactionId = paytrailTransactionId;
	}
}

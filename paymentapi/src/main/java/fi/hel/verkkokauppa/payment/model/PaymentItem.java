package fi.hel.verkkokauppa.payment.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

@Document(indexName = "payment_item")
public class PaymentItem {

	@Id
	String paymentItemId;

	@Field(type = FieldType.Keyword)
	String paymentId;

	@Field(type = FieldType.Keyword)
	String orderId;

	@Field(type = FieldType.Keyword)
	String productId;

	@Field(type = FieldType.Text)
	String productName;

	@Field(type = FieldType.Text)
	Integer quantity;

	@Field(type = FieldType.Double)
	BigDecimal rowPriceNet;

	@Field(type = FieldType.Double)
	BigDecimal rowPriceVat;

	@Field(type = FieldType.Double)
	BigDecimal rowPriceTotal;

	@Field(type = FieldType.Text)
	String taxPercent;

	@Field(type = FieldType.Double)
	BigDecimal taxAmount;

	@Field(type = FieldType.Double)
	BigDecimal priceNet;

	@Field(type = FieldType.Double)
	BigDecimal priceGross;

	public String getPaymentItemId() {
		return paymentItemId;
	}

	public void setPaymentItemId(String paymentItemId) {
		this.paymentItemId = paymentItemId;
	}

	public String getPaymentId() {
		return paymentId;
	}

	public void setPaymentId(String paymentId) {
		this.paymentId = paymentId;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getRowPriceNet() {
		return rowPriceNet;
	}

	public void setRowPriceNet(BigDecimal rowPriceNet) {
		this.rowPriceNet = rowPriceNet;
	}

	public BigDecimal getRowPriceVat() {
		return rowPriceVat;
	}

	public void setRowPriceVat(BigDecimal rowPriceVat) {
		this.rowPriceVat = rowPriceVat;
	}

	public BigDecimal getRowPriceTotal() {
		return rowPriceTotal;
	}

	public void setRowPriceTotal(BigDecimal rowPriceTotal) {
		this.rowPriceTotal = rowPriceTotal;
	}

	public String getTaxPercent() {
		return taxPercent;
	}

	public void setTaxPercent(String taxPercent) {
		this.taxPercent = taxPercent;
	}

	public BigDecimal getTaxAmount() {
		return taxAmount;
	}

	public void setTaxAmount(BigDecimal taxAmount) {
		this.taxAmount = taxAmount;
	}

	public BigDecimal getPriceNet() {
		return priceNet;
	}

	public void setPriceNet(BigDecimal priceNet) {
		this.priceNet = priceNet;
	}

	public BigDecimal getPriceGross() {
		return priceGross;
	}

	public void setPriceGross(BigDecimal priceGross) {
		this.priceGross = priceGross;
	}
}

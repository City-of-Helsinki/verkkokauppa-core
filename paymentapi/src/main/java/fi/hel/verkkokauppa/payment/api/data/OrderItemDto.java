package fi.hel.verkkokauppa.payment.api.data;

import java.math.BigDecimal;

public class OrderItemDto {

	private String orderItemId;
	private String orderId;
	private String productId;
	private String productName;
	private Integer quantity;
	private String unit;
	private BigDecimal rowPriceNet;
	private BigDecimal rowPriceVat;
	private BigDecimal rowPriceTotal;

	public String getOrderItemId() {
		return orderItemId;
	}

	public void setOrderItemId(String orderItemId) {
		this.orderItemId = orderItemId;
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

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
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
}

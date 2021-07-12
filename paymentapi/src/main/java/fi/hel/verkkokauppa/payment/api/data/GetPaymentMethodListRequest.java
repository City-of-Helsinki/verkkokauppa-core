package fi.hel.verkkokauppa.payment.api.data;

import java.math.BigDecimal;

public class GetPaymentMethodListRequest {

	private BigDecimal totalPrice;
	private String currency = "EUR";
	private String namespace;

	public BigDecimal getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(BigDecimal totalPrice) {
		this.totalPrice = totalPrice;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
}

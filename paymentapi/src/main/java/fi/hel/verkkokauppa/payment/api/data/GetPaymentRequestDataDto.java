package fi.hel.verkkokauppa.payment.api.data;

public class GetPaymentRequestDataDto {

	private OrderWrapper order;
	private String paymentMethod;
	private String language;

	public OrderWrapper getOrder() {
		return order;
	}

	public void setOrder(OrderWrapper order) {
		this.order = order;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}
}

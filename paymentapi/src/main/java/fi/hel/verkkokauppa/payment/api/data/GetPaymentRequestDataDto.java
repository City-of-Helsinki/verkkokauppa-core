package fi.hel.verkkokauppa.payment.api.data;

public class GetPaymentRequestDataDto {

	private OrderWrapper order;
	private String paymentMethod; // TODO: is this needed?

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
}

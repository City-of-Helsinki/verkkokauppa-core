package fi.hel.verkkokauppa.payment.api.data;

public class OrderPaymentDto {

	// TODO: needed?
	String orderId;
	String paymentType;
	String namespace;
	String sum;
	String description;

	public OrderPaymentDto() {}

	public OrderPaymentDto(String orderId, String paymentType, String namespace, String sum, String description) {
		this.orderId = orderId;
		this.paymentType = paymentType;
		this.namespace = namespace;
		this.sum = sum;
		this.description = description;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public String getPaymentType() {
		return paymentType;
	}

	public void setPaymentType(String paymentType) {
		this.paymentType = paymentType;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getSum() {
		return sum;
	}

	public void setSum(String sum) {
		this.sum = sum;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}

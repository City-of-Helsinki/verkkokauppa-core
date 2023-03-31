package fi.hel.verkkokauppa.payment.api.data;

import lombok.Data;

@Data
public class OrderDto {

	private String orderId;
	private String namespace;
	private String user;
	private String createdAt;
	private String status;
	private String type;
	private String customerFirstName;
	private String customerLastName;
	private String customerEmail;
	private String priceNet;
	private String priceVat;
	private String priceTotal;

}

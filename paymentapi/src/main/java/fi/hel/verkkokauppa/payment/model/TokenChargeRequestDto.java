package fi.hel.verkkokauppa.payment.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenChargeRequestDto {

	// TALPA order id
	private String orderId;
	// TALPA order item id (row id)
	private String orderItemId;
	// TALPA product id
	private String productId;
	// product id for external system
	private String namespaceEntityId;
	// quantity bought
	private Integer quantity;
	// Amount bought
	private String amount;
	// Generated Bar-/QR code
	private String token;

}

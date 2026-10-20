package fi.hel.verkkokauppa.order.model.voucher;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class TokenChargeRequestDto {

	// TALPA order id
//	private String orderId; // not used for now
	// TALPA order item id (row id)
	private String order_item_id;
	// TALPA product id
//	private String productId; // not used for now
	// product id for external system
	private String access_right_template_id;
	// quantity bought
//	private Integer quantity;
	// Amount bought
//	private String amount;
	// Generated Bar-/QR code
//	private String token;
}

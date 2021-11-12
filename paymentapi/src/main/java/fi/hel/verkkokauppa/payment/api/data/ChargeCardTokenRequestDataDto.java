package fi.hel.verkkokauppa.payment.api.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ChargeCardTokenRequestDataDto {

	public String namespace;
	public String orderId;
	public String orderItemId;
	public String productName;
	public String productQuantity;
	public String priceTotal;
	public String priceNet;
	public String vatPercentage;
	public String cardToken;

}

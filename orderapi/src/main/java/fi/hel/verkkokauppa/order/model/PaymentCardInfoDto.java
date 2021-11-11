package fi.hel.verkkokauppa.order.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentCardInfoDto {

	private String cardToken;
	private Short expYear;
	private Byte expMonth;

}

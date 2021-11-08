package fi.hel.verkkokauppa.order.api.data.subscription;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class PaymentCardInfoDto {

	private String cardToken;
	private Short expYear;
	private Byte expMonth;

}

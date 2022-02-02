package fi.hel.verkkokauppa.payment.api.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentCardInfoDto {

	private String cardToken;
	private Short expYear;
	private Byte expMonth;
	private String cardLastFourDigits;

}

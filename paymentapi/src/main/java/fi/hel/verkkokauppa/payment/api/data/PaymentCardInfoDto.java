package fi.hel.verkkokauppa.payment.api.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentCardInfoDto {

	private String cardToken;
	private Short expYear;
	private Byte expMonth;
	private String cardLastFourDigits;

	public PaymentCardInfoDto(String token, String expYear, String expMonth, String lastFourDigits) {
		this.cardToken = token;
		this.expYear = Short.valueOf(expYear);
		this.expMonth = Byte.valueOf(expMonth);
		this.cardLastFourDigits = lastFourDigits;
	}
}

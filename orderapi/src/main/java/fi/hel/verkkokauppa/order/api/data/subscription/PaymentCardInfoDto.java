package fi.hel.verkkokauppa.order.api.data.subscription;

import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
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
	private String cardLastFourDigits;

	public static PaymentCardInfoDto fromPaymentMessage(PaymentMessage message) {
		// Create paymentCardInfo from payment message
		return new PaymentCardInfoDto(
				message.getEncryptedCardToken(), // Encrypted in message
				message.getCardTokenExpYear(),
				message.getCardTokenExpMonth(),
				message.getCardLastFourDigits()
		);
	}

	public static PaymentCardInfoDto fromSubscriptionMessage(SubscriptionMessage message) {
		return new PaymentCardInfoDto(
				message.getEncryptedCardToken(), // Encrypted in message
				Short.valueOf(message.getCardTokenExpYear()),
				Byte.valueOf(message.getCardTokenExpMonth()),
				message.getCardLastFourDigits()
		);
	}
}

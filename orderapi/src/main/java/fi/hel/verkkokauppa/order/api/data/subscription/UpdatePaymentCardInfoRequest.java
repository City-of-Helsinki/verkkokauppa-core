package fi.hel.verkkokauppa.order.api.data.subscription;

import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class UpdatePaymentCardInfoRequest {

	private String subscriptionId;
	private PaymentCardInfoDto paymentCardInfoDto;
	private String user;

	public static UpdatePaymentCardInfoRequest fromSubscriptionMessage(SubscriptionMessage message) {
		return new UpdatePaymentCardInfoRequest(
				message.getSubscriptionId(),
				PaymentCardInfoDto.fromSubscriptionMessage(message),
				message.getUser()
		);
	}

}

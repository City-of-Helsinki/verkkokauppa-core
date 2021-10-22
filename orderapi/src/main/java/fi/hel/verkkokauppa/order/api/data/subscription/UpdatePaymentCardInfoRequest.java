package fi.hel.verkkokauppa.order.api.data.subscription;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePaymentCardInfoRequest {

	private String subscriptionId;
	private PaymentCardInfoDto paymentCardInfoDto;

}

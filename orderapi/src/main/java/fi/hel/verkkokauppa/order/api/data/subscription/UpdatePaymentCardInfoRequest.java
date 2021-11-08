package fi.hel.verkkokauppa.order.api.data.subscription;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class UpdatePaymentCardInfoRequest {

	private String subscriptionId;
	private PaymentCardInfoDto paymentCardInfoDto;
	private String user;

}

package fi.hel.verkkokauppa.payment.api.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetPaymentRequestDataDto {

	private OrderWrapper order;
	private String paymentMethod;
	private String paymentMethodLabel;
	private String language;
	private String merchantId;
}

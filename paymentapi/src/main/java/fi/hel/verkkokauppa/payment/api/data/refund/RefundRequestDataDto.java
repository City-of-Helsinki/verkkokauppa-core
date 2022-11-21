package fi.hel.verkkokauppa.payment.api.data.refund;

import fi.hel.verkkokauppa.common.rest.refund.RefundAggregateDto;
import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundRequestDataDto {

	private OrderWrapper order;
	private RefundAggregateDto refundAggregateDto;
	private String paymentMethod;
	private String paymentMethodLabel;
	private String language;
	private String merchantId;
}

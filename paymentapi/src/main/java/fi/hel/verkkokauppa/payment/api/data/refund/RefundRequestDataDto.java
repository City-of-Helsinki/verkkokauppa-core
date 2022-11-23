package fi.hel.verkkokauppa.payment.api.data.refund;

import fi.hel.verkkokauppa.common.rest.refund.RefundAggregateDto;
import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;
import fi.hel.verkkokauppa.payment.api.data.PaymentDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundRequestDataDto {
	private OrderWrapper order;
	private RefundAggregateDto refund;
	private PaymentDto payment;
	private String merchantId;
}

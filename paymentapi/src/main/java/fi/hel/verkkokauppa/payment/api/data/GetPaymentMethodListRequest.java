package fi.hel.verkkokauppa.payment.api.data;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GetPaymentMethodListRequest {

	private BigDecimal totalPrice;
	private String currency = "EUR";
	private String namespace;

	private OrderDto orderDto;

}

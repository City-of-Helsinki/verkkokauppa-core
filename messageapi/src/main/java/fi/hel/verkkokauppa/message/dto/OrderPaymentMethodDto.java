package fi.hel.verkkokauppa.message.dto;

import fi.hel.verkkokauppa.common.constants.PaymentGatewayEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaymentMethodDto {
	private String orderId;
	private String userId;
	private String name;
	private String code;
	private String group;
	private String img;
	private PaymentGatewayEnum gateway;
}

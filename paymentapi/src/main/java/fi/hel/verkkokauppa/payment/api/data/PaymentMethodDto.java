package fi.hel.verkkokauppa.payment.api.data;

import fi.hel.verkkokauppa.payment.constant.GatewayEnum;
import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class PaymentMethodDto {
	private String name;
	private String code;
	private String group;
	private String img;
	private GatewayEnum gateway;
}

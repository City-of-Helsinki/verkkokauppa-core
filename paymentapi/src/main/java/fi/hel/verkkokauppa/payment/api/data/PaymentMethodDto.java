package fi.hel.verkkokauppa.payment.api.data;

import fi.hel.verkkokauppa.payment.constant.PaymentGatewayEnum;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodDto {
	private String name;
	private String code;
	private String group;
	private String img;
	private PaymentGatewayEnum gateway;
}

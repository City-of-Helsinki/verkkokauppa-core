package fi.hel.verkkokauppa.payment.api.data;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class PaymentMethodDto {
	private String name;
	private String code;
	private String group;
	private String img;
}

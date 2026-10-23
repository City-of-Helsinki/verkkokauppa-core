package fi.hel.verkkokauppa.order.model.voucher;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class TokenChargeErrorResponseDto {

	Boolean result;
	private String[] errors;
}

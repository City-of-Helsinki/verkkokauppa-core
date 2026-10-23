package fi.hel.verkkokauppa.order.model.voucher;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Data
public class TokenChargeResponseDto {

	TokenChargeResponseResultDto result;
	private String[] errors;
}


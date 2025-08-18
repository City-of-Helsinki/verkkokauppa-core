package fi.hel.verkkokauppa.common.payment.dto;

import lombok.Data;

@Data
public class UpdateFromPaytrailRefundDto {
	private String refundId;
	private String merchantId;
//	private String paytrailTransactionId;
	private String namespace;
}

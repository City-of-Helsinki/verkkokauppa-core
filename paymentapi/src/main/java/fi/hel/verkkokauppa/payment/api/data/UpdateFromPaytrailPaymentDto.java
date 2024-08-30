package fi.hel.verkkokauppa.payment.api.data;

import lombok.Data;

@Data
public class UpdateFromPaytrailPaymentDto {
	private String paymentId;
	private String merchantId;
//	private String paytrailTransactionId;
	private String namespace;
}

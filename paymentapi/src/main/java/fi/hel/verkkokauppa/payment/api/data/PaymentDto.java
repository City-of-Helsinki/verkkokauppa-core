package fi.hel.verkkokauppa.payment.api.data;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentDto {

	private String paymentId;
	private String namespace;
	private String orderId;
	private String userId;
	private String status;
	private String paymentMethod;
	private String paymentType; // TODO: what is this?
	private BigDecimal totalExclTax;
	private BigDecimal total;
	private BigDecimal taxAmount;
	private String description; // TODO: needed?
	private String additionalInfo;
	private String token;
	private String timestamp;
	private String paymentMethodLabel;
	private String paytrailTransactionId;

	/**
	 * There is no way to determine in the paytrail return callback methods if a payment is shopInShop payment or not.
	 * This new field will be set on payment creation to indicate what type of paytrail flow is used for the specific payment.
	 */
	private boolean shopInShopPayment;

}

package fi.hel.verkkokauppa.payment.api.data;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentDto {

	String paymentId;
	String namespace;
	String orderId;
	String userId;
	String status;
	String paymentMethod;
	String paymentType; // TODO: what is this?
	BigDecimal totalExclTax;
	BigDecimal total;
	BigDecimal taxAmount;
	String description; // TODO: needed?
	String additionalInfo;
	String token;
	String timestamp;
	String paymentMethodLabel;
	String paytrailTransactionId;

	/**
	 * There is no way to determine in the paytrail return callback methods if a payment is shopInShop payment or not.
	 * This new field will be set on payment creation to indicate what type of paytrail flow is used for the specific payment.
	 */
	boolean shopInShopPayment;

}

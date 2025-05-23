package fi.hel.verkkokauppa.common.service.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CheckPaymentDto {

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
	private String paymentProviderStatus;
	private String paytrailMerchantId;
	private LocalDateTime createdAt;
	private LocalDateTime paidAt;

	/**
	 * There is no way to determine in the paytrail return callback methods if a payment is shopInShop payment or not.
	 * This new field will be set on payment creation to indicate what type of paytrail flow is used for the specific payment.
	 */
	private boolean shopInShopPayment;

}

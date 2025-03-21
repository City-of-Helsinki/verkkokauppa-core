package fi.hel.verkkokauppa.message.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentDto {
	String paymentId;
	String namespace;
	String orderId;
	String userId;
	String status;
	String paymentProviderStatus;
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
//	private PaytrailPaymentProviderModel paytrailProvider;
//	PaymentGatewayEnum paymentGateway;
	LocalDateTime createdAt;
	LocalDateTime updatedAt;
	LocalDateTime paidAt; // Timestamp when the transaction was paid
	public String getId() {
		return this.paymentId;
	}
}

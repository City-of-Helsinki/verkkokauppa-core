package fi.hel.verkkokauppa.common.rest.dto.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown=true)
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
	String createdAt;
	String updatedAt;
	String paidAt; // Timestamp when the transaction was paid
	public String getId() {
		return this.paymentId;
	}
}

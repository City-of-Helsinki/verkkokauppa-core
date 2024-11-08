package fi.hel.verkkokauppa.order.api.cron.search.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResultDto {
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

    boolean shopInShopPayment;

    String paymentGateway; // using string because index search cant use the real Payment model

    LocalDateTime createdAt;

    LocalDateTime updatedAt;

    LocalDateTime paidAt; // Timestamp when the transaction was paid

}

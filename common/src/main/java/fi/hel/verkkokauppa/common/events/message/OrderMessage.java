package fi.hel.verkkokauppa.common.events.message;

import fi.hel.verkkokauppa.common.constants.PaymentGatewayEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderMessage implements EventMessage {
    public String eventType;
    public String namespace;

    public String orderId;
    public String orderItemId;
    public String timestamp;

    public String orderType;

    public String productName;
    public String productQuantity;

    public String priceTotal;
    public String priceNet;
    public String priceVat;
    public String vatPercentage;

    public String cardToken;
    public PaymentGatewayEnum paymentGateway;

    public Boolean isSubscriptionRenewalOrder;
    public String subscriptionId;
    public String userId;

    // Fields required by paytrail mit charge
    public String merchantId;
    public String customerEmail;
    public String customerFirstName;
    public String customerLastName;
    public String productId;
    public String priceGross;
    public String cardExpYear;
    public String cardExpMonth;
    public String cardLastFourDigits;

    public OrderMessage toCustomerWebhook() {
        return OrderMessage
                .builder()
                .namespace(this.namespace)
                .orderId(this.orderId)
                .timestamp(this.timestamp)
                .eventType(this.eventType)
                .build();
    }

    public boolean isCardDefined() {
        return this.getCardToken() != null && this.getCardExpYear() != null &&
                this.getCardExpMonth() != null && this.getCardLastFourDigits() != null;
    }
}

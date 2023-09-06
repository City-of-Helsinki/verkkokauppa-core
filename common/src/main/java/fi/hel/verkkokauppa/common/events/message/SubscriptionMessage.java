package fi.hel.verkkokauppa.common.events.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import fi.hel.verkkokauppa.common.events.message.EventMessage;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionMessage implements EventMessage {
    String eventType;
    String eventTimestamp;
    String namespace;

    String subscriptionId;
    String orderId;
    String orderItemId;
    String timestamp;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String cancellationCause;

    String orderType;
    String encryptedCardToken;
    String cardTokenExpYear;
    String cardTokenExpMonth;
    String cardLastFourDigits;
    String user;

    public SubscriptionMessage toCustomerWebHook(){
        return SubscriptionMessage.builder()
                .subscriptionId(this.getSubscriptionId())
                .orderId(this.getOrderId())
                .orderItemId(this.getOrderItemId())
                .namespace(this.getNamespace())
                .eventType(this.getEventType())
                .cancellationCause(this.getCancellationCause())
                .timestamp(this.getTimestamp())
                .eventTimestamp(this.getEventTimestamp())
                .build();
    }
}

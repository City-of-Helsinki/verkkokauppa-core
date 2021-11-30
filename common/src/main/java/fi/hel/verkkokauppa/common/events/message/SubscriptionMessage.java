package fi.hel.verkkokauppa.common.events.message;

import fi.hel.verkkokauppa.common.events.message.EventMessage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionMessage implements EventMessage {
    String eventType;
    String namespace;

    String subscriptionId;
    String timestamp;

    String cancellationCause;

    public SubscriptionMessage toCustomerWebHook(){
        return SubscriptionMessage.builder()
                .subscriptionId(this.getSubscriptionId())
                .namespace(this.getNamespace())
                .eventType(this.getEventType())
                .cancellationCause(this.getCancellationCause())
                .timestamp(this.getTimestamp())
                .build();
    }
}

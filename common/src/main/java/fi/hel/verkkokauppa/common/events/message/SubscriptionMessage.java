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
    String namespace;

    String subscriptionId;
    String orderId;
    String timestamp;
    @JsonInclude(JsonInclude.Include.NON_NULL)
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

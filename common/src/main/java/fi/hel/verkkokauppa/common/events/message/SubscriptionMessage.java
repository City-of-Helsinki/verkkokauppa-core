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

}

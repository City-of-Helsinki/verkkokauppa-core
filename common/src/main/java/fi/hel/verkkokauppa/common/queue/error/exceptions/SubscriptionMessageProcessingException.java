package fi.hel.verkkokauppa.common.queue.error.exceptions;

import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public class SubscriptionMessageProcessingException extends RuntimeException {
    private SubscriptionMessage subscriptionMessage;
    public SubscriptionMessageProcessingException(String message, SubscriptionMessage subscriptionMessage) {
        super(message);
        this.subscriptionMessage = subscriptionMessage;
        log.error(message + subscriptionMessage.toString());
    }
}

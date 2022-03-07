package fi.hel.verkkokauppa.common.queue.error;

import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ErrorHandler;

@Slf4j
public class DefaultErrorHandler implements ErrorHandler {

    @Override
    public void handleError(Throwable t) {
        log.error(t.getCause().getMessage());
        if (t.getCause() instanceof SubscriptionMessageProcessingException) {
            SubscriptionMessage subscriptionMessage = ((SubscriptionMessageProcessingException) t.getCause()).getSubscriptionMessage();
            log.error(t.getCause().getMessage() + " {}", subscriptionMessage);
            // TODO Resend to queue?
        }

    }
}

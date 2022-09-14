package fi.hel.verkkokauppa.common.queue.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.queue.error.exceptions.PaymentMessageProcessingException;
import fi.hel.verkkokauppa.common.queue.error.exceptions.SubscriptionMessageProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ErrorHandler;

@Slf4j
public class DefaultActiveMQErrorHandler implements ErrorHandler {

    private ObjectMapper mapper;

    public DefaultActiveMQErrorHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void handleError(Throwable t) {
        try {
            log.error(mapper.writeValueAsString(t));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize error for logging.");
            log.error("Error cause: {}", t.getCause());
            log.error("Error message: {}", t.getMessage());
        }
        if (t.getCause() instanceof SubscriptionMessageProcessingException) {
            SubscriptionMessage subscriptionMessage = ((SubscriptionMessageProcessingException) t.getCause()).getSubscriptionMessage();
            log.error(t.getCause().getMessage() + " {}", subscriptionMessage);
        }

        if (t.getCause() instanceof PaymentMessageProcessingException) {
            PaymentMessage paymentMessage = ((PaymentMessageProcessingException) t.getCause()).getPaymentMessage();
            log.error(t.getCause().getMessage() + " {}", paymentMessage);
        }

    }
}

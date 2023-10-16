package fi.hel.verkkokauppa.common.queue.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.QueueConfigurations;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.events.message.RefundMessage;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.queue.error.exceptions.DLQPaymentMessageProcessingException;
import fi.hel.verkkokauppa.common.queue.error.exceptions.PaymentMessageProcessingException;
import fi.hel.verkkokauppa.common.queue.error.exceptions.RefundMessageProcessingException;
import fi.hel.verkkokauppa.common.queue.error.exceptions.SubscriptionMessageProcessingException;
import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ErrorHandler;

@Slf4j
public class DefaultActiveMQErrorHandler implements ErrorHandler {

    private ObjectMapper mapper;
    private SendNotificationService sendNotificationService;
    private QueueConfigurations queueConfigurations;

    public DefaultActiveMQErrorHandler(
            ObjectMapper mapper,
            SendNotificationService sendNotificationService,
            QueueConfigurations queueConfigurations
    ) {
        this.mapper = mapper;
        this.sendNotificationService = sendNotificationService;
        this.queueConfigurations = queueConfigurations;
    }

    @Override
    public void handleError(Throwable t) {
        try {
            log.error("DefaultActiveMqError: "+ mapper.writeValueAsString(t));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize error for logging.");
            log.error("Error cause: {}", e.getCause());
            log.error("Error message: {}", e.getMessage());
            log.error("Original message: {}", t.getMessage());
        }
        if (t.getCause() instanceof SubscriptionMessageProcessingException) {
            SubscriptionMessage subscriptionMessage = ((SubscriptionMessageProcessingException) t.getCause()).getSubscriptionMessage();
            log.error(t.getCause().getMessage() + " {}", subscriptionMessage);
        }

        if (t.getCause() instanceof PaymentMessageProcessingException) {
            PaymentMessage paymentMessage = ((PaymentMessageProcessingException) t.getCause()).getPaymentMessage();
            log.error(t.getCause().getMessage() + " {}", paymentMessage);
        }

        if (t.getCause() instanceof RefundMessageProcessingException) {
            RefundMessage refundMessage = ((RefundMessageProcessingException) t.getCause()).getRefundMessage();
            log.error(t.getCause().getMessage() + " {}", refundMessage);
        }

        if (t.getCause() instanceof DLQPaymentMessageProcessingException) {
            PaymentMessage paymentMessage = ((DLQPaymentMessageProcessingException) t.getCause()).getPaymentMessage();
            log.error(t.getCause().getMessage() + " {}", paymentMessage);
            sendNotificationService.sendToQueue(paymentMessage, queueConfigurations.getPaymentFailedToProcessQueue());
        }

    }
}

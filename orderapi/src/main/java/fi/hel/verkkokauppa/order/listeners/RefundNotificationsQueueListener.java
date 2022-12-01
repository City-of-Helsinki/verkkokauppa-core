package fi.hel.verkkokauppa.order.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.events.message.RefundMessage;
import fi.hel.verkkokauppa.common.history.service.SaveHistoryService;
import fi.hel.verkkokauppa.common.queue.error.exceptions.PaymentMessageProcessingException;
import fi.hel.verkkokauppa.common.queue.error.exceptions.RefundMessageProcessingException;
import fi.hel.verkkokauppa.common.rest.RestWebHookService;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.TextMessage;

@Component
@Slf4j
public class RefundNotificationsQueueListener {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private SaveHistoryService saveHistoryService;

    @Autowired
    private RestWebHookService restWebHookService;

    @JmsListener(destination = "${queue.refund.notifications:refund-notifications}")
    public void consumeMessage(TextMessage textMessage) throws Exception {
        log.info("Consuming refund-notifications message");
        RefundMessage message = getRefundMessageFromTextMessage(textMessage);

        logMessageData((ActiveMQTextMessage) textMessage, message);

        refundPaidAction(message);

        // Save history
        saveHistoryService.saveRefundMessageHistory(message);
    }

    /**
     * Sends RefundMessage as payload to merchant refund payment webhook url.
     * If response is not 200 then this function throws error code, and it will be redelivered 5 times back to queue.
     */
    private void refundPaidAction(RefundMessage message) throws JsonProcessingException {
        if (EventType.REFUND_PAID.equals(message.getEventType())) {
            ResponseEntity<Void> response = restWebHookService.postCallWebHook(
                    message.toCustomerWebHook(),
                    ServiceConfigurationKeys.MERCHANT_REFUND_WEBHOOK_URL,
                    message.getNamespace()
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RefundMessageProcessingException("Webhook call failed", message);
            }
        }
    }

    private RefundMessage getRefundMessageFromTextMessage(TextMessage textMessage) throws JMSException, JsonProcessingException {
        String jsonMessage = textMessage.getText();
        log.info(RefundNotificationsQueueListener.class + "{}", jsonMessage);

        return mapper.readValue(jsonMessage, RefundMessage.class);
    }

    /**
     * Logs redelivery count and refundId from RefundMessage
     */
    private void logMessageData(ActiveMQTextMessage textMessage, RefundMessage message) throws JsonProcessingException {
        log.info("ActiveMQ text message: {}", textMessage != null ? textMessage.toString() : null);
        log.info("Refund message: {}", mapper.writeValueAsString(message));
        log.info("Message refundId: {} } redeliveryCounter: {}", message.getRefundId(), textMessage.getRedeliveryCounter());
    }
}

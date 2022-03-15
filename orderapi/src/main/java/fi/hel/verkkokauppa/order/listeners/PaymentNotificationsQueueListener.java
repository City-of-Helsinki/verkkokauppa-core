package fi.hel.verkkokauppa.order.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.history.service.SaveHistoryService;
import fi.hel.verkkokauppa.common.queue.error.exceptions.PaymentMessageProcessingException;
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
public class PaymentNotificationsQueueListener {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private SaveHistoryService saveHistoryService;

    @Autowired
    private RestWebHookService restWebHookService;

    @JmsListener(destination = "${queue.payment.notifications:payment-notifications}")
    public void consumeMessage(TextMessage textMessage) throws Exception {
        PaymentMessage message = getPaymentMessageFromTextMessage(textMessage);

        this.logMessageData((ActiveMQTextMessage) textMessage, message);

        this.paymentPaidAction(message);

        // Save history
        saveHistoryService.savePaymentMessageHistory(message);
    }

    /**
     * Sends PaymentMessage as payload to merchant payment webhook url.
     * If response is not 200 then this function throws error code, and it will be redelivered 5 times back to queue.
     *
     */
    private void paymentPaidAction(PaymentMessage message) throws JsonProcessingException {
        if (EventType.PAYMENT_PAID.equals(message.getEventType())) {
            ResponseEntity<Void> response = restWebHookService.postCallWebHook(
                    message.toCustomerWebHook(),
                    ServiceConfigurationKeys.MERCHANT_PAYMENT_WEBHOOK_URL,
                    message.getNamespace()
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new PaymentMessageProcessingException("Webhook call failed", message);
            }
        }
    }

    private PaymentMessage getPaymentMessageFromTextMessage(TextMessage textMessage) throws JMSException, JsonProcessingException {
        String jsonMessage = textMessage.getText();
        log.info(PaymentNotificationsQueueListener.class + "{}", jsonMessage);

        return mapper.readValue(jsonMessage, PaymentMessage.class);
    }

    /**
     * Logs redelivery count and orderId from PaymentMessage
     */
    private void logMessageData(ActiveMQTextMessage textMessage, PaymentMessage message) throws JsonProcessingException {
        log.info(mapper.writeValueAsString(message));
        log.info("Message orderId: {} } redeliveryCounter: {}", message.getOrderId(),  textMessage.getRedeliveryCounter());
    }
}

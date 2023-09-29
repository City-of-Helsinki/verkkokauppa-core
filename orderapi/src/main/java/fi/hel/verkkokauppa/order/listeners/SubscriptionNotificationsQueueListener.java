package fi.hel.verkkokauppa.order.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ExperienceUrls;
import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.history.service.SaveHistoryService;
import fi.hel.verkkokauppa.common.queue.error.exceptions.SubscriptionMessageProcessingException;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
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
public class SubscriptionNotificationsQueueListener {
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private RestWebHookService restWebHookService;

    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private ExperienceUrls experienceUrls;

    @Autowired
    private SaveHistoryService saveHistoryService;

    @JmsListener(destination = "${queue.subscription.notifications:subscription-notifications}")
    public void consumeMessage(TextMessage textMessage) throws Exception {

        SubscriptionMessage message = getSubscriptionMessageFromTextMessage(textMessage);

        logMessageData((ActiveMQTextMessage) textMessage, message);
        // EventType.SUBSCRIPTION_CREATED
        subscriptionCreatedAction(message);

        // Save history
        saveHistoryService.saveSubscriptionMessageHistory(message);

    }

    private SubscriptionMessage getSubscriptionMessageFromTextMessage(TextMessage textMessage) throws JMSException, JsonProcessingException {
        String jsonMessage = textMessage.getText();
        log.info(SubscriptionNotificationsQueueListener.class + "{}", jsonMessage);

        return mapper.readValue(jsonMessage, SubscriptionMessage.class);
    }

    /**
     * Logs redelivery count and orderId and subscriptionId from SubscriptionMessage
     */
    private void logMessageData(ActiveMQTextMessage textMessage, SubscriptionMessage message) throws JsonProcessingException {
        log.info(mapper.writeValueAsString(message));
        log.info("Message orderId: {} subscriptionId: {} redeliveryCounter: {}", message.getOrderId(), message.getSubscriptionId(), textMessage.getRedeliveryCounter());
    }

    /**
     * Sends SubscriptionMessage as payload to merchant subscription webhook url.
     * If response is not 200 then this function throws error code, and it will be redelivered 5 times back to queue.
     *
     */
    private void subscriptionCreatedAction(SubscriptionMessage message) throws JsonProcessingException {
        if (EventType.SUBSCRIPTION_CREATED.equals(message.getEventType())) {
            log.info("event type is SUBSCRIPTION_CREATED");
            ResponseEntity<Void> response = restWebHookService.postCallWebHook(
                    message.toCustomerWebHook(),
                    ServiceConfigurationKeys.MERCHANT_SUBSCRIPTION_WEBHOOK_URL,
                    message.getNamespace()
            );
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new SubscriptionMessageProcessingException("Webhook call failed", message);
            }
        }
    }

    /**
     *
     */
    private void subscriptionCardExpiredAction(SubscriptionMessage message) throws JsonProcessingException {
        if (EventType.SUBSCRIPTION_CARD_EXPIRED.equals(message.getEventType())) {
            log.info("event type is {}", message.getEventType());

            restServiceClient.makeAdminGetCall(
                    experienceUrls.getOrderExperienceUrl()
                    + message.getSubscriptionId()
                    + "/emailSubscriptionCardExpired"
            );

        }
    }
}

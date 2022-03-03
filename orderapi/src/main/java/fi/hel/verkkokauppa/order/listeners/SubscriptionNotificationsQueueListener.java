package fi.hel.verkkokauppa.order.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.history.service.SaveHistoryService;
import fi.hel.verkkokauppa.common.queue.error.SubscriptionMessageProcessingException;
import fi.hel.verkkokauppa.common.rest.RestWebHookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SubscriptionNotificationsQueueListener {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private RestWebHookService restWebHookService;

    @Autowired
    private SaveHistoryService saveHistoryService;

    @JmsListener(destination = "${queue.subscription.notifications:subscription-notifications}")
    public void consumeMessage(String jsonMessage) {
        try {
            log.info(SubscriptionNotificationsQueueListener.class + "{}", jsonMessage);

            SubscriptionMessage message = mapper.readValue(jsonMessage, SubscriptionMessage.class);

            log.info(mapper.writeValueAsString(message));

            if (EventType.SUBSCRIPTION_CREATED.equals(message.getEventType())) {
                log.info("event type is SUBSCRIPTION_CREATED");
                ResponseEntity<Void> response = restWebHookService.postCallWebHook(
                        message,
                        ServiceConfigurationKeys.MERCHANT_SUBSCRIPTION_WEBHOOK_URL,
                        message.getNamespace()
                );
                if (response.getStatusCode() != HttpStatus.OK) {
                    throw new SubscriptionMessageProcessingException("Webhook call failed", message);
                }
            }
            // Save history
            saveHistoryService.saveSubscriptionMessageHistory(message);
        } catch (Exception e) {
            log.error(SubscriptionNotificationsQueueListener.class + " handling listened subscription notifications failed", e);
        }
    }
}

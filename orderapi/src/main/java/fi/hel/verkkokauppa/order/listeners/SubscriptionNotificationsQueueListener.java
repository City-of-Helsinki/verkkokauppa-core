package fi.hel.verkkokauppa.order.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SubscriptionNotificationsQueueListener {

    @Autowired
    private ObjectMapper mapper;

    @JmsListener(destination = "${queue.subscription.notifications:subscription-notifications}")
    public void consumeMessage(String jsonMessage) {
        try {
            log.info(SubscriptionNotificationsQueueListener.class +  "{}", jsonMessage);
            SubscriptionMessage message = mapper.readValue(jsonMessage, SubscriptionMessage.class);

            log.info(mapper.writeValueAsString(message));
            // TODO actions

        } catch (Exception e) {
            log.error(SubscriptionNotificationsQueueListener.class + " handling listened order notifications failed", e);
        }
    }
}

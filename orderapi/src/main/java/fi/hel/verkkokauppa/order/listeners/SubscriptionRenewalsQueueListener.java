package fi.hel.verkkokauppa.order.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SubscriptionRenewalsQueueListener {

    @Autowired
    private ObjectMapper mapper;

    @JmsListener(destination = "${queue.subscription.renewals:subscription-renewals}")
    public void consumeMessage(String jsonMessage) {
        try {
            log.info(SubscriptionRenewalsQueueListener.class +  "{}", jsonMessage);
            SubscriptionMessage message = mapper.readValue(jsonMessage, SubscriptionMessage.class);

            log.info(mapper.writeValueAsString(message));
            // TODO actions

        } catch (Exception e) {
            log.error(SubscriptionRenewalsQueueListener.class + " handling listened subscription renewals failed",  e);
        }
    }
}

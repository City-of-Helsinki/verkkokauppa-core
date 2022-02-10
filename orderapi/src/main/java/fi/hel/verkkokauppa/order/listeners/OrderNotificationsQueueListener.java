package fi.hel.verkkokauppa.order.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.QueueConfigurations;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderNotificationsQueueListener {


    @Autowired
    private ObjectMapper mapper;

    @JmsListener(destination = "${queue.order.notifications:order-notifications}")
    public void consumeMessage(String jsonMessage) {
        try {
            log.info(OrderNotificationsQueueListener.class +  "{}", jsonMessage);
            OrderMessage message = mapper.readValue(jsonMessage, OrderMessage.class);

            log.info(mapper.writeValueAsString(message));
            // TODO actions
        } catch (Exception e) {
            log.error(OrderNotificationsQueueListener.class + " handling listened order notifications failed", e);
        }
    }
}

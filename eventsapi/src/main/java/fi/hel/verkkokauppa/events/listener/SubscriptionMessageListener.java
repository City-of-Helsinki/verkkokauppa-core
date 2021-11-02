package fi.hel.verkkokauppa.events.listener;

import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.TopicName;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionMessageListener {

    private Logger log = LoggerFactory.getLogger(SubscriptionMessageListener.class);

    @Autowired
    private Environment env;

    @KafkaListener(
            topics = "subscriptions",
            groupId="subscriptions",
            containerFactory="subscriptionsKafkaListenerContainerFactory")
    void subscriptionCreatedlistener(SubscriptionMessage message) {
        log.info("subscriptionCreatedlistener [{}]", message);

        if (EventType.SUBSCRIPTION_CREATED.equals(message.getEventType())) {
            log.info("event type is SUBSCRIPTION_CREATED");
            // TODO action
        }

        String orderServiceUrl = env.getRequiredProperty("order.service.url");
        log.info("order.service.url is: " + orderServiceUrl);
    }

}

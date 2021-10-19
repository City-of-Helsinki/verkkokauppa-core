package fi.hel.events.listener;

import fi.hel.verkkokauppa.common.message.SubscriptionMessage;
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
            topics = "SUBSCRIPTION_CREATED",
            groupId="subscriptions",
            containerFactory="subscriptionsKafkaListenerContainerFactory")
    void subscriptionCreatedlistener(SubscriptionMessage message) {
        log.info("subscriptionCreatedlistener [{}]", message);
    }

}

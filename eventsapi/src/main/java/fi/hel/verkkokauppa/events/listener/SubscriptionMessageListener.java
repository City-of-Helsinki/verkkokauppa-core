package fi.hel.verkkokauppa.events.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.TopicName;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionMessageListener {

    private Logger log = LoggerFactory.getLogger(SubscriptionMessageListener.class);

    @Autowired
    private Environment env;

    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(
            topics = "subscriptions",
            groupId="events-api",
            containerFactory="subscriptionsKafkaListenerContainerFactory")
    void subscriptionCreatedlistener(String jsonMessage) {
        try {
            log.info("subscriptionCreatedlistener [{}]", jsonMessage);
            SubscriptionMessage message = objectMapper.readValue(jsonMessage, SubscriptionMessage.class);

            if (EventType.SUBSCRIPTION_CREATED.equals(message.getEventType())) {
                log.info("event type is SUBSCRIPTION_CREATED");
                // TODO action
            }

            String orderServiceUrl = env.getRequiredProperty("order.service.url");
            log.info("order.service.url is: " + orderServiceUrl);

        } catch (Exception e) {
            log.error("handling listened subscriptions event failed, jsonMessage: " + jsonMessage, e);
        }
    }

}

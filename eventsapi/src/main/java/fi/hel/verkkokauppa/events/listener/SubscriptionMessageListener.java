package fi.hel.verkkokauppa.events.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionMessageListener {

    private Logger log = LoggerFactory.getLogger(SubscriptionMessageListener.class);

    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${order.service.url}")
    private String orderServiceUrl;

    @Value("${order.experience.url}")
    private String orderExperienceUrl;


    @KafkaListener(
            topics = "subscriptions",
            groupId="events-api",
            containerFactory="subscriptionsKafkaListenerContainerFactory")
    void subscriptionEventlistener(String jsonMessage) {
        try {
            log.info("subscriptionCreatedlistener [{}]", jsonMessage);
            SubscriptionMessage message = objectMapper.readValue(jsonMessage, SubscriptionMessage.class);

            if (EventType.SUBSCRIPTION_CREATED.equals(message.getEventType())) {
                log.info("event type is SUBSCRIPTION_CREATED");
                subscriptionCreatedAction(message);
            } else if (EventType.SUBSCRIPTION_CANCELLED.equals(message.getEventType())) {
                log.info("event type is SUBSCRIPTION_CANCELLED");
                subscriptionCancelledAction(message);
            } else if (EventType.SUBSCRIPTION_RENEWAL_REQUESTED.equals(message.getEventType())) {
                log.info("event type is SUBSCRIPTION_RENEWAL_REQUESTED");
                subscriptionRenewalRequestedAction(message);
            } else if (EventType.SUBSCRIPTION_RENEWAL_VALIDATION_FAILED.equals(message.getEventType())) {
                log.info("event type is SUBSCRIPTION_RENEWAL_VALIDATION_FAILED");
                subscriptionRenewalValidationFailed(message);
            } else if (EventType.SUBSCRIPTION_UPDATE_CARD.equals(message.getEventType())) {
                log.info("event type is SUBSCRIPTION_UPDATE_CARD");
                subscriptionUpdateCardAction(message);
            }
        } catch (Exception e) {
            log.error("handling listened subscriptions event failed, jsonMessage: " + jsonMessage, e);
        }
    }

    private void subscriptionCreatedAction(SubscriptionMessage message) {
        String url = orderExperienceUrl + "subscription/" + message.getSubscriptionId() + "/emailSubscriptionContract";
        restServiceClient.makeAdminPostCall(url, "{}");
        // TODO action
    }

    private void subscriptionCancelledAction(SubscriptionMessage message) {
        callOrderApiWithPath(message, "/subscription/subscription-cancelled-webhook");
    }

    private void subscriptionRenewalRequestedAction(SubscriptionMessage message) {
        callOrderApiWithPath(message, "/subscription-admin/renewal-requested-event");
    }

    private void subscriptionRenewalValidationFailed(SubscriptionMessage message) {
        callOrderApiWithPath(message, "/subscription-admin/renewal-validation-failed");
    }

    private void subscriptionUpdateCardAction(SubscriptionMessage message) {
        callOrderApiWithPath(message,"/subscription/set-card-token-event");
    }

    private void callOrderApiWithPath(SubscriptionMessage message, String path) {
        try {
            String url = orderServiceUrl + path;
            callApi(message, url);
        } catch (Exception e) {
            log.error("failed action after receiving event, eventType: " + message.getEventType(), e);
        }
    }

    private void callApi(SubscriptionMessage message, String url) throws Exception {
        //format payload, message to json string conversion
        String body = objectMapper.writeValueAsString(message);
        //send to target url
        restServiceClient.makeVoidPostCall(url, body);
    }

}

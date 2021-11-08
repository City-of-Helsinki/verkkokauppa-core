package fi.hel.verkkokauppa.events.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;


@Component
public class PaymentMessageListener {

    private Logger log = LoggerFactory.getLogger(PaymentMessageListener.class);

    @Autowired
    private Environment env;

    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private ObjectMapper objectMapper;


    @KafkaListener(
            topics = "payments",
            groupId="events-api",
            containerFactory="paymentsKafkaListenerContainerFactory")
    private void paymentEventlistener(String jsonMessage) {
        try {
            log.info("paymentEventlistener [{}]", jsonMessage);
            PaymentMessage message = objectMapper.readValue(jsonMessage, PaymentMessage.class);

            if (EventType.PAYMENT_PAID.equals(message.getEventType())) {
                log.debug("event type is PAYMENT_PAID");
                paymentPaidAction(message);
            }
            else if (EventType.PAYMENT_FAILED.equals(message.getEventType())) {
                log.debug("event type is PAYMENT_FAILED");
                paymentFailedAction(message);
            }
        } catch (Exception e) {
            log.error("handling listened payments event failed, jsonMessage: " + jsonMessage, e);
        }
    }

    private void paymentPaidAction(PaymentMessage message) {
        try {
            //read target url to call from env
            String url = env.getRequiredProperty("subscription.service.url");
            log.debug("subscription.service.url is: " + url);

            //format payload, message to json string conversion
            String body = objectMapper.writeValueAsString(message);

            //send to target url
            restServiceClient.makePostCall(url, body);
        } catch (Exception e) {
            log.error("failed action after receiving event, eventType: " + message.getEventType(), e);
        }
    }

    private void paymentFailedAction(PaymentMessage message) {
        // TODO action
        log.debug("TODO no action yet for PAYMENT_FAILED event");

    }


}
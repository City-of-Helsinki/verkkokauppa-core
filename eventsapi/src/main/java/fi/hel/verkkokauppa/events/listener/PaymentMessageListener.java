package fi.hel.verkkokauppa.events.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;


@Component
public class PaymentMessageListener {

    private Logger log = LoggerFactory.getLogger(PaymentMessageListener.class);

    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${order.service.url}")
    private String orderServiceUrl;


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
            String orderType = message.getOrderType();

            if (OrderType.SUBSCRIPTION.equals(orderType)) {
                String url = orderServiceUrl + "/subscription/payment-paid-event";
                callApi(message, url);
            } else if (OrderType.ORDER.equals(orderType)) {
                String url = orderServiceUrl + "/order/payment-paid-event";
                callApi(message, url);
            }
        } catch (Exception e) {
            log.error("failed action after receiving event, eventType: " + message.getEventType(), e);
        }
    }

    private void paymentFailedAction(PaymentMessage message) {
        try {
            String orderType = message.getOrderType();

            if (OrderType.SUBSCRIPTION.equals(orderType)) {
                String url = orderServiceUrl + "/subscription/payment-failed-event";
                callApi(message, url);
            } else if (OrderType.ORDER.equals(orderType)) {
                String url = orderServiceUrl + "/order/payment-failed-event";
                callApi(message, url);
            }
        } catch (Exception e) {
            log.error("failed action after receiving event, eventType: " + message.getEventType(), e);
        }
    }

    private void callApi(PaymentMessage message, String url) throws Exception {
        //format payload, message to json string conversion
        String body = objectMapper.writeValueAsString(message);
        //send to target url
        restServiceClient.makePostCall(url, body);
    }

}
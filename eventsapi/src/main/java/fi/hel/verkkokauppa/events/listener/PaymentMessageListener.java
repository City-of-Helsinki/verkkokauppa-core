package fi.hel.verkkokauppa.events.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
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
            groupId = "events-api",
            containerFactory = "paymentsKafkaListenerContainerFactory")
    private void paymentEventlistener(String jsonMessage) {
        try {
            log.info("paymentEventlistener [{}]", jsonMessage);
            PaymentMessage message = objectMapper.readValue(jsonMessage, PaymentMessage.class);
            log.debug("event type is {}", message.getEventType());
            if (EventType.PAYMENT_PAID.equals(message.getEventType())) {
                paymentPaidAction(message);
                orderPaidWebHookAction(message);
            } else if (EventType.PAYMENT_FAILED.equals(message.getEventType())) {
                paymentFailedAction(message);
            } else if (EventType.SUBSCRIPTION_CARD_RENEWAL_CREATED.equals(message.getEventType())) {
                paymentCardRenewalAction(message);
            }
        } catch (Exception e) {
            log.error("handling listened payments event failed, jsonMessage: " + jsonMessage, e);
        }
    }

    protected void orderPaidWebHookAction(PaymentMessage message) {
        try {
            callOrderApi(message,
                    "/order/payment-paid-webhook",
                    "/subscription/payment-paid-webhook");
        } catch (Exception e) {
            log.error("webhookAction: failed action after receiving event, eventType: " + message.getEventType(), e);
        }
    }

    private void callOrderApi(PaymentMessage message, String orderPath, String subscriptionPath) throws Exception {
        String orderType = message.getOrderType();
        if (OrderType.SUBSCRIPTION.equals(orderType)) {
            String url = orderServiceUrl + subscriptionPath;
            callApi(message, url);
        } else if (OrderType.ORDER.equals(orderType)) {
            String url = orderServiceUrl + orderPath;
            callApi(message, url);
        }
    }

    private void paymentPaidAction(PaymentMessage message) {
        try {
            callOrderApi(message,
                    "/order/payment-paid-event",
                    "/subscription/payment-paid-event");
        } catch (Exception e) {
            log.error("failed action after receiving event, eventType: " + message.getEventType(), e);
        }
    }

    private void paymentFailedAction(PaymentMessage message) {
        try {
            callOrderApi(message,
                    "/order/payment-failed-event",
                    "/subscription/payment-failed-event");
        } catch (Exception e) {
            log.error("failed action after receiving event, eventType: " + message.getEventType(), e);
        }
    }

    private void paymentCardRenewalAction(PaymentMessage message) {
        try {
            callApi(message, orderServiceUrl + "/subscription/payment-update-card");
        } catch (Exception e) {
            log.error("failed action after receiving event, eventType: {}", message.getEventType(), e);
        }
    }

    private void callApi(PaymentMessage message, String url) throws Exception {
        //format payload, message to json string conversion
        String body = objectMapper.writeValueAsString(message);
        //send to target url
        restServiceClient.makeVoidPostCall(url, body);
    }

}
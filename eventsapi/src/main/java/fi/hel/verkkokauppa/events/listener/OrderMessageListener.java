package fi.hel.verkkokauppa.events.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderMessageListener {

    private Logger log = LoggerFactory.getLogger(OrderMessageListener.class);

    @Autowired
    private Environment env;

    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    @KafkaListener(
            topics = "orders",
            groupId="events-api",
            containerFactory="ordersKafkaListenerContainerFactory")
    void orderEventlistener(String jsonMessage) {
        try {
            log.info("orderCreatedlistener [{}]", jsonMessage);
            OrderMessage message = objectMapper.readValue(jsonMessage, OrderMessage.class);

            if (EventType.ORDER_CREATED.equals(message.getEventType())) {
                log.info("event type is ORDER_CREATED");
                orderCreatedAction(message);
            }

            String paymentServiceUrl = env.getRequiredProperty("payment.service.url");
            log.info("payment.service.url is: " + paymentServiceUrl);

        } catch (Exception e) {
            log.error("handling listened orders event failed, jsonMessage: " + jsonMessage, e);
        }
    }

    private void orderCreatedAction(OrderMessage message) {
        try {
            String url = paymentServiceUrl + "/payment/order-created-event";
            callApi(message, url);
        } catch (Exception e) {
            log.error("failed action after receiving event, eventType: " + message.getEventType(), e);
        }
    }

    private void callApi(OrderMessage message, String url) throws Exception {
        //format payload, message to json string conversion
        String body = objectMapper.writeValueAsString(message);
        //send to target url
        restServiceClient.makePostCall(url, body);
    }
}

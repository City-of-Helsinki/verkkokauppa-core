package fi.hel.verkkokauppa.events.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.constants.PaymentGatewayEnum;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.common.rest.RestWebHookService;
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

    @Autowired
    private RestWebHookService restWebHookService;

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    @Value("${order.service.url}")
    private String orderServiceUrl;

    @KafkaListener(
            topics = "orders",
            groupId="events-api",
            containerFactory="ordersKafkaListenerContainerFactory")
    void orderEventlistener(String jsonMessage) {
        try {
            log.info("orderCreatedlistener [{}]", jsonMessage);
            OrderMessage message = objectMapper.readValue(jsonMessage, OrderMessage.class);

            // Already moved to ActiveMQ
            if (EventType.SUBSCRIPTION_RENEWAL_ORDER_CREATED.equals(message.getEventType())) {
                log.info("event type is SUBSCRIPTION_RENEWAL_ORDER_CREATED");
                subscriptionRenewalOrderCreatedAction(message);
            }
            // TODO: needs to be moved to ActiveMQ
            if (EventType.ORDER_CANCELLED.equals(message.getEventType())) {
                log.info("event type is {}", message.getEventType());
                triggerOrderWebhook(message, "/order/order-cancelled-webhook");
            }

        } catch (Exception e) {
            log.error("handling listened orders event failed, jsonMessage: " + jsonMessage, e);
        }
    }

    private void subscriptionRenewalOrderCreatedAction(OrderMessage message) {
        try {
            String url = message.getPaymentGateway() != null && message.getPaymentGateway().equals(PaymentGatewayEnum.PAYTRAIL) ?
                    paymentServiceUrl + "/payment-admin/paytrail/subscription-renewal-order-created-event" :
                    paymentServiceUrl + "/payment-admin/subscription-renewal-order-created-event";
            callApi(message, url);
            restWebHookService.postCallWebHook(message.toCustomerWebhook(), ServiceConfigurationKeys.MERCHANT_ORDER_WEBHOOK_URL, message.getNamespace());
        } catch (Exception e) {
            log.error("failed action after receiving event, eventType: " + message.getEventType(), e);
        }
    }

    private void triggerOrderWebhook(OrderMessage message, String webhookTriggerPath) {
        try {
            String url = orderServiceUrl + webhookTriggerPath;
            callApi(message, url);
        } catch (Exception e) {
            log.error("failed action after receiving event, eventType: " + message.getEventType(), e);
        }
    }

    private void callApi(OrderMessage message, String url) throws Exception {
        //format payload, message to json string conversion
        String body = objectMapper.writeValueAsString(message);
        //send to target url
        restServiceClient.makeVoidPostCall(url, body);
    }
}

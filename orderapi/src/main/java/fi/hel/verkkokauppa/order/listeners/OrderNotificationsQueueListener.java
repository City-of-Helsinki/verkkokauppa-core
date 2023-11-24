package fi.hel.verkkokauppa.order.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.constants.PaymentGatewayEnum;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.history.service.SaveHistoryService;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.common.rest.RestWebHookService;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.TextMessage;

@Component
@Slf4j
public class OrderNotificationsQueueListener {


    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private SaveHistoryService saveHistoryService;

    @Value("${payment.service.url}")
    private String paymentServiceUrl;

    @Autowired
    private RestWebHookService restWebHookService;

    @Autowired
    private RestServiceClient restServiceClient;

    @JmsListener(destination = "${queue.order.notifications:order-notifications}")
    public void consumeMessage(TextMessage textMessage) throws Exception {
        log.info("Consuming order-notifications message");
        OrderMessage message = getOrderMessageFromTextMessage(textMessage);

        logMessageData((ActiveMQTextMessage) textMessage, message);

        if (message.getEventType().equals(EventType.SUBSCRIPTION_RENEWAL_ORDER_CREATED)) {
            subscriptionRenewalOrderCreatedAction(message);
        }

        // Save history
        saveHistoryService.saveOrderMessageHistory(message);
    }

    private OrderMessage getOrderMessageFromTextMessage(TextMessage textMessage) throws JMSException, JsonProcessingException {
        String jsonMessage = textMessage.getText();
        log.info(OrderNotificationsQueueListener.class + "{}", jsonMessage);

        return mapper.readValue(jsonMessage, OrderMessage.class);
    }

    /**
     * Logs redelivery count and orderId and subscriptionId from OrderMessage
     */
    private void logMessageData(ActiveMQTextMessage textMessage, OrderMessage message) throws JsonProcessingException {
        log.info("ActiveMQ text message: {}", textMessage != null ? textMessage.toString() : null);
        log.info("SubscriptionOrder message: {}", mapper.writeValueAsString(message));
        log.info("Message orderId: {} subscriptionId: {} redeliveryCounter: {}", message.getOrderId(), message.getSubscriptionId(), textMessage.getRedeliveryCounter());
    }

    private void subscriptionRenewalOrderCreatedAction(OrderMessage message) throws Exception {
        Exception orderWebhookException = null;
        try {
            restWebHookService.postCallWebHook(message.toCustomerWebhook(), ServiceConfigurationKeys.MERCHANT_ORDER_WEBHOOK_URL, message.getNamespace());
        } catch (Exception e)
        {
            // catch exception and try to renew order even if subscription order created webhook call fails
            orderWebhookException = e;
        }
        String url = message.getPaymentGateway() != null && message.getPaymentGateway().equals(PaymentGatewayEnum.PAYTRAIL) ?
                paymentServiceUrl + "/payment-admin/paytrail/subscription-renewal-order-created-event" :
                paymentServiceUrl + "/payment-admin/subscription-renewal-order-created-event";
        callApi(message, url);

        if ( orderWebhookException != null ){
            log.info("Rethrowing subscription renewal order created webhook call exception");
            throw orderWebhookException;
        }

    }

    private void callApi(OrderMessage message, String url) throws Exception {
        //format payload, message to json string conversion
        String body = mapper.writeValueAsString(message);
        //send to target url
        restServiceClient.makeVoidPostCall(url, body);
    }
}

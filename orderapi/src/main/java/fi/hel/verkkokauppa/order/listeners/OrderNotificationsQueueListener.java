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
import fi.hel.verkkokauppa.common.util.EncryptorUtil;
import fi.hel.verkkokauppa.common.util.StringUtils;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionStatus;
import fi.hel.verkkokauppa.order.service.subscription.GetSubscriptionQuery;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.time.LocalDateTime;

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

    @Autowired
    private GetSubscriptionQuery getSubscriptionQuery;

    @Value("${payment.card_token.encryption.password}")
    private String cardTokenEncryptionPassword;

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
        // multiple calls to this can create new orders even if subscription was just renewed
        // check that end date for order is not suspiciously far in the future (KYV-1202)
        LocalDateTime endDate = message.getEndDate();
        if( endDate.isAfter(LocalDateTime.now().plusDays(40)))
        {
            log.info("End date for orderId: {} is {}. Do not pay yet.", message.getOrderId(), endDate);
            // TODO: error notifikaatti
        }
        else {
            restWebHookService.postCallWebHook(message.toCustomerWebhook(), ServiceConfigurationKeys.MERCHANT_ORDER_WEBHOOK_URL, message.getNamespace());
            Subscription subscription = null;

            if (StringUtils.isNotEmpty(message.subscriptionId) && StringUtils.isNotEmpty(message.userId)) {
                subscription = getSubscriptionQuery.findByIdValidateByUser(message.subscriptionId, message.userId);
            }

            if (subscription != null && subscription.getStatus().equalsIgnoreCase(SubscriptionStatus.ACTIVE)) {
                log.info("Subscription: {} was active so sending SUBSCRIPTION_RENEWAL_ORDER_CREATED to paymentApi", message.subscriptionId);
                log.info("Subscription: {} card token was: {}", message.subscriptionId, message.getCardToken());
                message.setCardToken(subscription.getPaymentMethodToken());
                log.info("Subscription: {} card token was set to: {}", message.subscriptionId, message.getCardToken());
                String url = message.getPaymentGateway() != null && message.getPaymentGateway().equals(PaymentGatewayEnum.PAYTRAIL) ?
                        paymentServiceUrl + "/payment-admin/paytrail/subscription-renewal-order-created-event" :
                        paymentServiceUrl + "/payment-admin/subscription-renewal-order-created-event";
                callApi(message, url);
            } else {
                log.info("Subscription: {} was in status {} so skipping payment handling", message.subscriptionId, subscription.getStatus());
            }
        }
    }

    private void callApi(OrderMessage message, String url) throws Exception {
        //format payload, message to json string conversion
        String body = mapper.writeValueAsString(message);
        //send to target url
        restServiceClient.makeVoidPostCall(url, body, message.getNamespace());
    }
}

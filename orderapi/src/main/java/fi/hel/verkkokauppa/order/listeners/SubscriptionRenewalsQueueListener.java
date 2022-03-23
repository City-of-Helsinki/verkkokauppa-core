package fi.hel.verkkokauppa.order.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.history.service.SaveHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.TextMessage;

@Component
@Slf4j
public class SubscriptionRenewalsQueueListener {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private SaveHistoryService saveHistoryService;

    @JmsListener(destination = "${queue.subscription.renewals:subscription-renewals}")
    public void consumeMessage(TextMessage textMessage) throws Exception {

        SubscriptionMessage message = getSubscriptionMessageFromTextMessage(textMessage);

        logMessageData((ActiveMQTextMessage) textMessage, message);
        // TODO Actions

        // Save history
        saveHistoryService.saveSubscriptionMessageHistory(message);
    }

    private SubscriptionMessage getSubscriptionMessageFromTextMessage(TextMessage textMessage) throws JMSException, JsonProcessingException {
        String jsonMessage = textMessage.getText();
        log.info(SubscriptionNotificationsQueueListener.class + "{}", jsonMessage);

        return mapper.readValue(jsonMessage, SubscriptionMessage.class);
    }

    /**
     * Logs redelivery count and orderId and subscriptionId from SubscriptionMessage
     */
    private void logMessageData(ActiveMQTextMessage textMessage, SubscriptionMessage message) throws JsonProcessingException {
        log.info(mapper.writeValueAsString(message));
        log.info("Message orderId: {} subscriptionId: {} redeliveryCounter: {}", message.getOrderId(), message.getSubscriptionId(), textMessage.getRedeliveryCounter());
    }

}

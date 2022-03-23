package fi.hel.verkkokauppa.order.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
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
public class OrderNotificationsQueueListener {


    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private SaveHistoryService saveHistoryService;

    @JmsListener(destination = "${queue.order.notifications:order-notifications}")
    public void consumeMessage(TextMessage textMessage) throws Exception {
        OrderMessage message = getOrderMessageFromTextMessage(textMessage);

        logMessageData((ActiveMQTextMessage) textMessage, message);
        // TODO actions

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
        log.info(mapper.writeValueAsString(message));
        log.info("Message orderId: {} subscriptionId: {} redeliveryCounter: {}", message.getOrderId(), message.getSubscriptionId(), textMessage.getRedeliveryCounter());
    }
}

package fi.hel.verkkokauppa.common.queue.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.QueueConfigurations;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.*;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class SendNotificationService {
    /**
     * Injection of spring boot encapsulated tool class
     */
    @Resource
    private JmsTemplate jmsTemplate;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private QueueConfigurations queueConfigurations;

    public void sendToQueue(
            EventMessage message,
            String toQueue
    ) {
        try {
            ActiveMQQueue queue = new ActiveMQQueue(toQueue);
            String messageAsJsonString = mapper.writeValueAsString(message);
            log.info("Received notification to queue {} message {}", toQueue, messageAsJsonString);
            jmsTemplate.convertAndSend(queue, messageAsJsonString);
        } catch (Exception e) {
            log.error("Error sending to queue: {} error message: {}", toQueue, e.getMessage());
        }

    }

    public void sendSubscriptionMessageNotification(
            SubscriptionMessage message
    ) {
        String toQueue = queueConfigurations.getSubscriptionNotificationsQueue();
        try {
            ActiveMQQueue queue = new ActiveMQQueue(toQueue);
            String messageAsJsonString = mapper.writeValueAsString(message);
            log.info("Received notification to queue {} message {}", toQueue, messageAsJsonString);
            jmsTemplate.convertAndSend(queue, messageAsJsonString);
        } catch (Exception e) {
            log.error("Error sending to queue: {} error message: {}", toQueue, e.getMessage());
        }

    }

    public void sendOrderMessageNotification(OrderMessage message) {
        String toQueue = queueConfigurations.getOrderNotificationsQueue();
        try {
            ActiveMQQueue queue = new ActiveMQQueue(toQueue);
            String messageAsJsonString = mapper.writeValueAsString(message);
            log.info("Sending notification to queue {} message {}", toQueue, messageAsJsonString);
            jmsTemplate.convertAndSend(queue, messageAsJsonString);
        } catch (Exception e) {
            log.error("Error sending to queue: {} error message: {}", toQueue, e.getMessage());
        }
    }

    public void sendPaymentMessageNotification(
            PaymentMessage message
    ) {
        String toQueue = queueConfigurations.getPaymentNotificationsQueue();
        try {
            ActiveMQQueue queue = new ActiveMQQueue(toQueue);
            String messageAsJsonString = mapper.writeValueAsString(message);
            log.info("Received notification to queue {} message {}", toQueue, messageAsJsonString);
            jmsTemplate.convertAndSend(queue, messageAsJsonString, msg -> {
                msg.setStringProperty("MsgType", message.getEventType());
                return msg;
            });
        } catch (Exception e) {
            log.error("Error sending to queue: {} error message: {}", toQueue, e.getMessage());
        }

    }

    public void sendRefundMessageNotification(
            RefundMessage message
    ) {
        String toQueue = queueConfigurations.getRefundNotificationsQueue();
        try {
            ActiveMQQueue queue = new ActiveMQQueue(toQueue);
            String messageAsJsonString = mapper.writeValueAsString(message);
            log.info("Received notification to queue {} message {}", toQueue, messageAsJsonString);
            jmsTemplate.convertAndSend(queue, messageAsJsonString, msg -> {
                msg.setStringProperty("MsgType", message.getEventType());
                return msg;
            });
        } catch (Exception e) {
            log.error("Error sending to queue: {} error message: {}", toQueue, e.getMessage());
        }

    }

    public void sendErrorNotification(
            String message,
            Exception exception
    ) {
        sendErrorNotification(
                message + " " + exception.getMessage(),
                exception.getStackTrace().toString()
        );
    }

    public void sendErrorNotification(
            String message,
            String cause
    ) {
        String toQueue = queueConfigurations.getErrorEmailNotificationsQueue();
        try {
            ActiveMQQueue queue = new ActiveMQQueue(toQueue);
            ErrorMessage queueMessage = ErrorMessage.builder()
                    .eventType(EventType.ERROR_EMAIL_NOTIFICATION)
                    .eventTimestamp(DateTimeUtil.getDateTime())
                    .message(message)
                    .cause(cause)
                    .build();
            String messageAsJsonString = mapper.writeValueAsString(queueMessage);
            log.info("Received notification to queue {} message {}", toQueue, messageAsJsonString);
            jmsTemplate.convertAndSend(queue, messageAsJsonString);
        } catch (Exception e) {
            log.error("Error sending to queue: {} error message: {}", toQueue, e.getMessage());
        }
    }
}

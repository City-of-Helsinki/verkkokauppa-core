package fi.hel.verkkokauppa.order.api.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.QueueConfigurations;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.jms.*;
import java.util.List;
import java.util.Objects;

@RestController
@Slf4j
public class ProduceMessageController {

    /**
     * Injection of spring boot encapsulated tool class
     */
    @Resource
    private JmsMessagingTemplate jmsTemplate;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private QueueConfigurations queueConfigurations;

    @GetMapping(value = "queue/getAll", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> queueGetAll() {
        try {
            return ResponseEntity.ok(queueConfigurations.getAll());
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-all-queues", "")
            );
        }
    }

    @PostMapping(value = "queue/send/order-message")
    public OrderMessage sendOrderMessage(
            @RequestBody OrderMessage orderMessage,
            @RequestParam(value = "toQueue") String toQueue
    ) {
        try {
            ActiveMQQueue queue = new ActiveMQQueue(toQueue);
            String orderMessageAsJson = mapper.writeValueAsString(orderMessage);

            jmsTemplate.convertAndSend(queue, orderMessageAsJson);
        } catch (Exception e) {
            log.error("/queue/send/order-message error {}", e.getMessage());
        }
        return orderMessage;
    }

    @PostMapping(value = "queue/send/subscription-message")
    public SubscriptionMessage sendMessage(
            @RequestBody SubscriptionMessage subscriptionMessage,
            @RequestParam(value = "toQueue") String toQueue
    ) {
        try {
            ActiveMQQueue queue = new ActiveMQQueue(toQueue);

            String messageAsJson = mapper.writeValueAsString(subscriptionMessage);

            jmsTemplate.convertAndSend(queue, messageAsJson);
        } catch (Exception e) {
            log.error("/queue/send/subscription-message error {}", e.getMessage());
        }
        return subscriptionMessage;
    }


    @PostMapping(value = "queue/send/payment-message")
    public PaymentMessage sendPaymentMessage(
            @RequestBody PaymentMessage paymentMessage,
            @RequestParam(value = "toQueue") String toQueue
    ) {
        try {
            ActiveMQQueue queue = new ActiveMQQueue(toQueue);
            String orderMessageAsJson = mapper.writeValueAsString(paymentMessage);

            jmsTemplate.convertAndSend(queue, orderMessageAsJson);
        } catch (Exception e) {
            log.error("/queue/send/payment-message error {}", e.getMessage());
        }
        return paymentMessage;
    }
}

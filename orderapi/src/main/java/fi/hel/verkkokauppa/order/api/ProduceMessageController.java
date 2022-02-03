package fi.hel.verkkokauppa.order.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;

import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.broker.region.Topic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.jms.Queue;

@RestController
@Slf4j
public class ProduceMessageController {

    /**
     * Injection of spring boot encapsulated tool class
     */
    @Resource
    private JmsMessagingTemplate jmsTemplate;

    @Resource
    @Qualifier("queueOrderNotifications")
    private Queue singleQueue;

    @Autowired
    private ObjectMapper mapper;


    @PostMapping(value = "/queue/order-message")
    public OrderMessage sendMessage(@RequestBody OrderMessage orderMessage) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String orderMessageAsJson = mapper.writeValueAsString(orderMessage);

            jmsTemplate.convertAndSend(singleQueue, orderMessageAsJson);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return orderMessage;
    }
}

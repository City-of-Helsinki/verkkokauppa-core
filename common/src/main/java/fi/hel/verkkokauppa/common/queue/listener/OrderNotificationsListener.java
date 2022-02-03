package fi.hel.verkkokauppa.common.queue.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.history.service.SaveHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderNotificationsListener {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private SaveHistoryService saveHistoryService;

    @JmsListener(destination = "${queue.order.notifications:order-notifications}")
    public void consumeMessage(String message) {
        log.info("Message received from activemq queue---" + message);
        try {
            OrderMessage orderMessage = mapper.readValue(message, OrderMessage.class);
            saveHistoryService.saveOrderMessageHistory(orderMessage);
        } catch (JsonProcessingException e) {
            log.error(OrderNotificationsListener.class + " consumeMessage json processing error", e);
        }
    }
}

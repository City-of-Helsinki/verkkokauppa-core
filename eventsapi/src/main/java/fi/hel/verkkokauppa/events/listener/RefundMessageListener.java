package fi.hel.verkkokauppa.events.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.RefundMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
public class RefundMessageListener {

    private Logger log = LoggerFactory.getLogger(RefundMessageListener.class);

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(
            topics = "refunds",
            groupId = "events-api",
            containerFactory = "refundsKafkaListenerContainerFactory")
    private void refundEventlistener(String jsonMessage) {
        try {
            log.info("refundEventlistener [{}]", jsonMessage);
            RefundMessage message = objectMapper.readValue(jsonMessage, RefundMessage.class);
            log.debug("event type is {}", message.getEventType());
            if (EventType.REFUND_CREATED.equals(message.getEventType())) {

            }
        } catch (Exception e) {
            log.error("handling listened refund event failed, jsonMessage: " + jsonMessage, e);
        }
    }
}
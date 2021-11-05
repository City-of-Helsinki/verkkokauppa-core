package fi.hel.verkkokauppa.common.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.events.message.EventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class SendEventService {
    private KafkaTemplate<String, String> kafkaTemplate;
    private KafkaTemplate<String, EventMessage> eventMessageKafkaTemplate;

    private Logger log = LoggerFactory.getLogger(SendEventService.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    SendEventService(KafkaTemplate<String, String> kafkaTemplate, KafkaTemplate<String, EventMessage> eventMessageKafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventMessageKafkaTemplate = eventMessageKafkaTemplate;
    }

    public void sendString(String topicName, String message) {
        kafkaTemplate.send(topicName, message);
    }

    public void sendEventMessage(String topicName, EventMessage message) {
        try {
            log.debug("sending to topic: " + topicName);

            //format payload, message to json string conversion
            String body = objectMapper.writeValueAsString(message);
            log.debug("message converted to JSON: " + body);

            // TODO send once, either as string or eventmessage
            kafkaTemplate.send(topicName, body);
            //eventMessageKafkaTemplate.send(topicName, message);

        } catch (Exception e) {
            log.error("failed to send event message to topic: " + topicName, e);
        }
    }

}
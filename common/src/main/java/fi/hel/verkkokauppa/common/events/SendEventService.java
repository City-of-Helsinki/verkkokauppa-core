package fi.hel.verkkokauppa.common.events;

import fi.hel.verkkokauppa.common.events.message.EventMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class SendEventService {
    private KafkaTemplate<String, String> kafkaTemplate;
    private KafkaTemplate<String, EventMessage> eventMessageKafkaTemplate;

    @Autowired
    SendEventService(KafkaTemplate<String, String> kafkaTemplate, KafkaTemplate<String, EventMessage> eventMessageKafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventMessageKafkaTemplate = eventMessageKafkaTemplate;
    }

    public void sendString(String topicName, String message) {
        kafkaTemplate.send(topicName, message);
    }

    public void sendEventMessage(String topicName, EventMessage message) {
        eventMessageKafkaTemplate.send(topicName, message);
    }

}
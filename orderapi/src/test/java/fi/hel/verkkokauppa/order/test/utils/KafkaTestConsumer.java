package fi.hel.verkkokauppa.order.test.utils;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

@Component
public class KafkaTestConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumer.class);

    private CountDownLatch latch = new CountDownLatch(1);
    private ConsumerRecord<?, ?> payload = null;

//    @KafkaListener(topics = "${test.topic}", groupId="${test.groupId}")
    public void receive(ConsumerRecord<?, ?> consumerRecord) {
        LOGGER.info("received payload='{}'", consumerRecord.toString());
        setPayload(consumerRecord);
        latch.countDown();
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public ConsumerRecord<?, ?> getPayload() {
        return payload;
    }

    public void setPayload(ConsumerRecord<?, ?> payload) {
        this.payload = payload;
    }
}

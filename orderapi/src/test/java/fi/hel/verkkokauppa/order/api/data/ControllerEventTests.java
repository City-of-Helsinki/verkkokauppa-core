package fi.hel.verkkokauppa.order.api.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.SendEventService;
import fi.hel.verkkokauppa.common.events.TopicName;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionCancellationCause;
import fi.hel.verkkokauppa.order.service.subscription.CancelSubscriptionCommand;
import fi.hel.verkkokauppa.order.service.subscription.CreateOrderFromSubscriptionCommand;
import fi.hel.verkkokauppa.order.test.utils.KafkaTestConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
        "test.topic=subscriptions",
        "test.groupId=subscriptions",
        "kafka.client.authentication.enabled=false"
})
public class ControllerEventTests {
    private Logger log = LoggerFactory.getLogger(ControllerEventTests.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaTestConsumer kafkaTestConsumer;

    @Autowired
    private SendEventService sendEventService;

    @Mock
    Environment env;

    // add to your application.properties spring.kafka.bootstrap-servers=localhost:9092
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Before
    public void beforeMethod() {
        // Test kafka only when in local environment.
        Assume.assumeTrue(bootstrapServers.contains("localhost:9092") || bootstrapServers.contains("host.docker.internal:9092"));
        // rest of setup.
    }

    //This test is ignored because uses pure kafka and not mocks to make testing easier when developing
//    @Test
    public void testSubscriptionCancelledEvent() throws JsonProcessingException, InterruptedException {
        SubscriptionMessage message = SubscriptionMessage.builder()
                .subscriptionId("1234")
                .namespace("TEST_NAMESPACE")
                .eventType(EventType.SUBSCRIPTION_CANCELLED)
                .cancellationCause(SubscriptionCancellationCause.CUSTOMER_CANCELLED)
                .timestamp(DateTimeUtil.getDateTime())
                .build();

        sendEventService.sendEventMessage(TopicName.SUBSCRIPTIONS, message);
        Boolean bool = kafkaTestConsumer.getLatch().await(10000, TimeUnit.MILLISECONDS);
        ConsumerRecord<?, ?> record = kafkaTestConsumer.getPayload();
        assertEquals(kafkaTestConsumer.getLatch().getCount(), 0L);
        String input = record.value().toString();
        String result = trimJsonString(input);

        SubscriptionMessage fromConsumer = objectMapper.readValue(result, SubscriptionMessage.class);
        log.info(String.valueOf(fromConsumer));
        assertEquals(fromConsumer.getSubscriptionId(), message.getSubscriptionId());
        assertEquals(fromConsumer.getNamespace(), message.getNamespace());
        assertEquals(fromConsumer.getEventType(), EventType.SUBSCRIPTION_CANCELLED);
        assertEquals(fromConsumer.getCancellationCause(), message.getCancellationCause());
        assertNotNull(fromConsumer.getTimestamp());

    }

    //This test is ignored because uses pure kafka and not mocks to make testing easier when developing
    // [KYV-405]
    //@Test
    public void testSendSubscriptionCancelledEvent() {
        SubscriptionMessage message = SubscriptionMessage.builder()
                .subscriptionId("1234")
                .namespace("venepaikat")
                .eventType(EventType.SUBSCRIPTION_CANCELLED)
                .cancellationCause(SubscriptionCancellationCause.CUSTOMER_CANCELLED)
                .timestamp(DateTimeUtil.getDateTime())
                .build();

        sendEventService.sendEventMessage(TopicName.SUBSCRIPTIONS, message);
    }



    public String trimJsonString(String input) {
        String result = input.substring(1, input.length() - 1);
        result = result.replace("\\\"","\"");
        return result;
    }

}
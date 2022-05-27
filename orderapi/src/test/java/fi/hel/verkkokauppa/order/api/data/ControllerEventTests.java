package fi.hel.verkkokauppa.order.api.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceUrls;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.SendEventService;
import fi.hel.verkkokauppa.common.events.TopicName;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.history.dto.HistoryDto;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionCancellationCause;
import fi.hel.verkkokauppa.order.test.utils.KafkaTestConsumer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
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

    @Autowired
    private RestServiceClient restServiceClient;

    @Autowired
    private ServiceUrls serviceUrls;


    // add to your application.properties spring.kafka.bootstrap-servers=localhost:9092
    @Value("${spring.kafka.bootstrap-servers:#{null}}")
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
        String namespace = UUIDGenerator.generateType4UUID().toString();
        SubscriptionMessage message = SubscriptionMessage.builder()
                .subscriptionId("1234")
                .namespace(namespace)
                .eventType(EventType.SUBSCRIPTION_CANCELLED)
                .cancellationCause(SubscriptionCancellationCause.CUSTOMER_CANCELLED)
                .timestamp(DateTimeUtil.getDateTime())
                .build();

        sendEventService.sendEventMessage(TopicName.SUBSCRIPTIONS, message);
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        // Waited 1 secs
        String eventType = EventType.SUBSCRIPTION_CANCELLED;

        HistoryDto dto = getFirstHistoryDto(namespace, eventType);

        SubscriptionMessage fromConsumer = objectMapper.readValue(dto.getPayload(), SubscriptionMessage.class);

        assertEquals(fromConsumer.getSubscriptionId(), message.getSubscriptionId());
        assertEquals(fromConsumer.getNamespace(), message.getNamespace());
        assertEquals(fromConsumer.getEventType(), EventType.SUBSCRIPTION_CANCELLED);
        assertEquals(fromConsumer.getCancellationCause(), message.getCancellationCause());
        assertNotNull(fromConsumer.getTimestamp());

    }

    private HistoryDto getFirstHistoryDto(String namespace, String eventType) throws JsonProcessingException {
        JSONObject result2 = restServiceClient.makeGetCall(
                serviceUrls.getHistoryServiceUrl() +
                        "/admin/history/list/get-event-type?" +
                        "namespace=" + namespace +
                        "&eventType=" + eventType
        );
        JSONArray histories = result2.getJSONArray("histories");
        HistoryDto dto = objectMapper.readValue(histories.getJSONObject(0).toString(), HistoryDto.class);
        return dto;
    }

    //This test is ignored because uses pure kafka and not mocks to make testing easier when developing
    // [KYV-405]
//    @Test
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
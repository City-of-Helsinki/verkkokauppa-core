package fi.hel.verkkokauppa.payment.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.repository.PaymentRepository;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import fi.hel.verkkokauppa.payment.utils.KafkaTestConsumer;
import fi.hel.verkkokauppa.payment.utils.TestPaymentCreator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
        "test.topic=payments",
        "test.groupId=payments",
        "kafka.client.authentication.enabled=false"
})
public class OnlinePaymentControllerTests {

    @Autowired
    private OnlinePaymentController onlinePaymentController;
    @Autowired
    private OnlinePaymentService onlinePaymentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private KafkaTestConsumer kafkaTestConsumer;

    @Mock
    Environment env;
    private Payment payment;

    // add to your application.properties spring.kafka.bootstrap-servers=localhost:9092
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Before
    public void beforeMethod() {
        // Test kafka only when in local environment.
        org.junit.Assume.assumeTrue(bootstrapServers.contains("localhost:9092") || bootstrapServers.contains("host.docker.internal:9092"));
        // rest of setup.
    }

    //This test is ignored because uses pure kafka and not mocks to make testing easier when developing
    @Test
    public void testUpdatePaymentStatus() throws JsonProcessingException, InterruptedException {
        payment = TestPaymentCreator.getDummyPayment("5e9c9784-4856-354b-8e2a-2d89de749249", "dummy_user", "venepaikat");
        //paymentRepository.save(payment);
        onlinePaymentController.triggerPaymentPaidEvent(payment);
        Boolean bool = kafkaTestConsumer.getLatch().await(10000, TimeUnit.MILLISECONDS);
        ConsumerRecord<?, ?> record = kafkaTestConsumer.getPayload();
        assertEquals(kafkaTestConsumer.getLatch().getCount(), 0L);

        PaymentMessage fromConsumer = objectMapper.readValue(record.value().toString(), PaymentMessage.class);

        assertEquals(fromConsumer.getPaymentId(), payment.getPaymentId());
        assertEquals(fromConsumer.getNamespace(), payment.getNamespace());
        assertEquals(fromConsumer.getOrderId(), payment.getOrderId());
        assertEquals(fromConsumer.getUserId(), payment.getUserId());
        assertEquals(fromConsumer.getOrderType(), payment.getPaymentType());
        assertNotNull(fromConsumer.getPaymentPaidTimestamp());

        assertEquals(fromConsumer.getEventType(), EventType.PAYMENT_PAID);

    }

}
package fi.hel.verkkokauppa.payment.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderItemDto;
import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;
import fi.hel.verkkokauppa.payment.logic.visma.VismaGetPaymentRequest;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.repository.PaymentRepository;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import fi.hel.verkkokauppa.payment.utils.KafkaTestConsumer;
import fi.hel.verkkokauppa.payment.utils.TestPaymentCreator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.helsinki.vismapay.response.payment.PaymentDetailsResponse;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
        "test.topic=payments",
        "test.groupId=payments",
        "kafka.client.authentication.enabled=false"
})
public class OnlinePaymentControllerTests {
    private final Logger log = LoggerFactory.getLogger(OnlinePaymentControllerTests.class);

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

    @Autowired
    private VismaGetPaymentRequest vismaPayment;

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
    @Ignore
    public void testUpdatePaymentStatus() throws JsonProcessingException, InterruptedException {
        payment = TestPaymentCreator.getDummyPayment("5e9c9784-4856-354b-8e2a-2d89de749249", "dummy_user", "venepaikat");
        //paymentRepository.save(payment);
        onlinePaymentService.triggerPaymentPaidEvent(payment);
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


    @Test
    public void testCreateCardRenewalPayment() throws IOException {
        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();

        OrderDto orderDto = new OrderDto();
        String orderId = "1234";
        orderDto.setOrderId(orderId);
        orderDto.setNamespace("venepaikat");
        orderDto.setUser("dummy_user");
        orderDto.setCreatedAt("");
        orderDto.setStatus("confirmed");
        orderDto.setType("subscription");
        orderDto.setCustomerFirstName("severi");
        orderDto.setCustomerLastName("ku");
        orderDto.setCustomerEmail("testi@ambientia.fi");
        orderDto.setPriceNet("1");
        orderDto.setPriceVat("0");
        // Sets total price to be 1 eur
        orderDto.setPriceTotal("1");
                OrderWrapper order = new OrderWrapper();
        order.setOrder(orderDto);

        List<OrderItemDto> items = new ArrayList<>();

        items.add(onlinePaymentService.getCardRenewalOrderItem(orderId));

        order.setItems(items);
        paymentRequestDataDto.setOrder(order);

        ResponseEntity<Payment> paymentResponseEntity = onlinePaymentController.createCardRenewalPayment(paymentRequestDataDto);

        Payment payment = paymentResponseEntity.getBody();

        String paymentUrl = onlinePaymentService.getPaymentUrl(payment.getToken());
        log.debug(paymentUrl);
        String paymentId = Objects.requireNonNull(payment).getPaymentId();
        //TESTING
        paymentId = "1234_at_20211220-105922";

        PaymentDetailsResponse getPaymentDetails = vismaPayment.getPaymentDetails(
                paymentId
        );
        // Create refund using visma pay
/*        Current status of the payment. Possible values are:
        Value	Explanation
        0	Incoming - payment is still in progress.
        1	Authorized - payment has been authorized but it hasn't been captured.
        2	Authorization failed - payment was not completed succesfully or it was declined.
        3	Validation failed - payment has failed due to validation error.
        4	Settled - payment has been completed succesfully.
        5	Paid - payment has been completed succesfully and it has been paid to the merchant.
        14	Reversed - authorization of a transaction has been canceled.
        15	Forwarded Paid - payment has been completed succesfully and it will be paid to the merchant by the provider of the payment method.*/

    }
}
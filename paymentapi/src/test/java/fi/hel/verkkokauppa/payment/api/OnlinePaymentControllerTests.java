package fi.hel.verkkokauppa.payment.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderItemDto;
import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;
import fi.hel.verkkokauppa.payment.logic.fetcher.CancelPaymentFetcher;
import fi.hel.verkkokauppa.payment.logic.visma.VismaAuth;
import fi.hel.verkkokauppa.payment.logic.fetcher.GetPaymentRequestFetcher;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.PaymentStatus;
import fi.hel.verkkokauppa.payment.repository.PaymentRepository;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import fi.hel.verkkokauppa.payment.utils.KafkaTestConsumer;
import fi.hel.verkkokauppa.payment.utils.TestPaymentCreator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.helsinki.vismapay.request.payment.CapturePaymentRequest;
import org.helsinki.vismapay.response.VismaPayResponse;
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
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
    private PaymentAdminController paymentAdminController;
    @Autowired
    private OnlinePaymentService onlinePaymentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private KafkaTestConsumer kafkaTestConsumer;

    @Autowired
    private GetPaymentRequestFetcher vismaPayment;
    @Autowired
    private CancelPaymentFetcher cancelPaymentFetcher;

    @Mock
    Environment env;

    private Payment payment;

    @Autowired
    private VismaAuth vismaAuth;

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
    @Ignore
    public void testCreateCardRenewalPayment() throws IOException, InterruptedException, ExecutionException {
        GetPaymentRequestDataDto paymentRequestDataDto = new GetPaymentRequestDataDto();

        OrderDto orderDto = new OrderDto();
        String orderId = UUID.randomUUID().toString();
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

        ResponseEntity<String> paymentUrl = paymentAdminController.createCardRenewalPayment(paymentRequestDataDto);

        ResponseEntity<List<Payment>> payments = paymentAdminController.getPayments(orderId, order.getOrder().getNamespace(), PaymentStatus.AUTHORIZED);

        log.debug(paymentUrl.getBody());

        String paymentId = Objects.requireNonNull(payments.getBody()).get(0).getPaymentId();
        //TESTING
//        paymentId = "1234_at_20211220-105922";


        PaymentDetailsResponse getPaymentDetails = vismaPayment.getPaymentDetails(
                paymentId
        );

        assertEquals(getPaymentDetails.getPayment().getAmount(), new BigDecimal("100"));
        assertEquals(getPaymentDetails.getPayment().getStatus(), (short) 0);
        // Cancel payment
        //VismaPayResponse responseCF2 = cancelPaymentFetcher.cancelPayment(paymentId);
        //assertEquals(responseCF2.getResult(),0, "Cancel to payment is not working.");
    }
}
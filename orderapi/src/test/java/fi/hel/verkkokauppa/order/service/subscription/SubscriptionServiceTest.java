package fi.hel.verkkokauppa.order.service.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.rest.RestServiceClient;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionIdsDto;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.subscription.Period;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionStatus;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import fi.hel.verkkokauppa.order.test.utils.TestUtils;
import fi.hel.verkkokauppa.order.testing.annotations.RunIfProfile;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@RunIfProfile(profile = "local")
class SubscriptionServiceTest extends TestUtils {
    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CreateOrderFromSubscriptionCommand createOrderFromSubscriptionCommand;

    @Autowired
    private GetSubscriptionQuery getSubscriptionQuery;

//    @Autowired
//    private RestServiceClient restServiceClient;
//
//    @MockBean
//    private RestServiceClient restServiceClientMock;

    private Order foundOrder;
    private Subscription foundSubscription;

//    @AfterEach
//    void tearDown() {
//        orderRepository.delete(foundOrder);
//        subscriptionRepository.delete(foundSubscription);
//    }

    @Test
    public void assertTrue(){
        Assertions.assertTrue(true);
    }

    @Test
    @RunIfProfile(profile = "local")
    void setOrderStartAndEndDate() {
        ResponseEntity<OrderAggregateDto> orderResponse = generateSubscriptionOrderData(1, 1L, Period.DAILY, 2);
        ResponseEntity<SubscriptionIdsDto> subscriptionIds = createSubscriptions(orderResponse);
        Optional<Order> order = orderRepository.findById(Objects.requireNonNull(orderResponse.getBody()).getOrder().getOrderId());
        Optional<Subscription> subscription = subscriptionRepository.findById(Objects.requireNonNull(subscriptionIds.getBody().getSubscriptionIds()).iterator().next());

        if (order.isPresent() && subscription.isPresent()) {
            foundOrder = order.get();
            foundSubscription = subscription.get();
            PaymentMessage paymentMessage = new PaymentMessage();
            paymentMessage.setPaymentPaidTimestamp(DateTimeUtil.getDateTime());

            orderService.setOrderStartAndEndDate(foundOrder, foundSubscription, paymentMessage);
            subscriptionService.setSubscriptionEndDateFromOrder(foundOrder, foundSubscription);
            Assertions.assertEquals(foundSubscription.getEndDate(), foundOrder.getEndDate());
        }
    }

    @Test
    @RunIfProfile(profile = "local")
    void createFromSubscription() {
        ResponseEntity<OrderAggregateDto> orderResponse = generateSubscriptionOrderData(1, 1L, Period.DAILY, 2);
        ResponseEntity<SubscriptionIdsDto> subscriptionIds = createSubscriptions(orderResponse);
        Optional<Order> order = orderRepository.findById(Objects.requireNonNull(orderResponse.getBody()).getOrder().getOrderId());
        Optional<Subscription> subscription = subscriptionRepository.findById(Objects.requireNonNull(subscriptionIds.getBody().getSubscriptionIds()).iterator().next());

        if (order.isPresent() && subscription.isPresent()) {
            foundOrder = order.get();
            foundSubscription = subscription.get();
            PaymentMessage paymentMessage = new PaymentMessage();
            paymentMessage.setPaymentPaidTimestamp(DateTimeUtil.getDateTime());

            orderService.setOrderStartAndEndDate(foundOrder, foundSubscription, paymentMessage);
            // set subscription end date into past
            foundSubscription.setEndDate(LocalDateTime.now().minusDays(10));
            subscriptionRepository.save(foundSubscription);

            SubscriptionDto subscriptionDto = getSubscriptionQuery.mapToDto(foundSubscription);
            String orderId = createOrderFromSubscriptionCommand.createFromSubscription(subscriptionDto);
            Assertions.assertEquals(foundOrder.getOrderId(), orderId);

            subscriptionService.setSubscriptionEndDateFromOrder(foundOrder, foundSubscription);
            Assertions.assertEquals(foundSubscription.getEndDate(), foundOrder.getEndDate());

            // creates new order because existing order has same end date as subscription
            String orderId2 = createOrderFromSubscriptionCommand.createFromSubscription(subscriptionDto);

            Optional<Order> order2 = orderRepository.findById(Objects.requireNonNull(orderId2));
            Assertions.assertTrue(order2.isPresent());
            Assertions.assertNotEquals(orderId, orderId2);
            foundOrder = order2.get();
            LocalDateTime datetime = foundSubscription.getEndDate();
            datetime = datetime.plus(1, ChronoUnit.DAYS);
            datetime = datetime.with(ChronoField.NANO_OF_DAY, LocalTime.MIDNIGHT.toNanoOfDay());
            Assertions.assertEquals(datetime, foundOrder.getStartDate());
            datetime = datetime.plus(1, ChronoUnit.DAYS);
            Assertions.assertEquals(datetime, foundOrder.getEndDate());
        }

    }

    @Test
    @RunIfProfile(profile = "local")
    void createFromSubscriptionWithoutOrderItemMetas() {
        ResponseEntity<OrderAggregateDto> orderResponse = generateSubscriptionOrderData(1, 1L, Period.DAILY, 2, false);
        ResponseEntity<SubscriptionIdsDto> subscriptionIds = createSubscriptions(orderResponse);
        Optional<Order> order = orderRepository.findById(Objects.requireNonNull(orderResponse.getBody()).getOrder().getOrderId());
        Optional<Subscription> subscription = subscriptionRepository.findById(Objects.requireNonNull(subscriptionIds.getBody().getSubscriptionIds()).iterator().next());

        if (order.isPresent() && subscription.isPresent()) {
            foundOrder = order.get();
            foundSubscription = subscription.get();
            PaymentMessage paymentMessage = new PaymentMessage();
            paymentMessage.setPaymentPaidTimestamp(DateTimeUtil.getDateTime());

            orderService.setOrderStartAndEndDate(foundOrder, foundSubscription, paymentMessage);
            // set subscription end date into past
            foundSubscription.setEndDate(LocalDateTime.now().minusDays(10));
            subscriptionRepository.save(foundSubscription);

            SubscriptionDto subscriptionDto = getSubscriptionQuery.mapToDto(foundSubscription);
            String orderId = createOrderFromSubscriptionCommand.createFromSubscription(subscriptionDto);
            Assertions.assertEquals(foundOrder.getOrderId(), orderId);

            subscriptionService.setSubscriptionEndDateFromOrder(foundOrder, foundSubscription);
            Assertions.assertEquals(foundSubscription.getEndDate(), foundOrder.getEndDate());

            // creates new order because existing order has same end date as subscription
            String orderId2 = createOrderFromSubscriptionCommand.createFromSubscription(subscriptionDto);

            Optional<Order> order2 = orderRepository.findById(Objects.requireNonNull(orderId2));
            Assertions.assertTrue(order2.isPresent());
            Assertions.assertNotEquals(orderId, orderId2);
            foundOrder = order2.get();
            LocalDateTime datetime = foundSubscription.getEndDate();
            datetime = datetime.plus(1, ChronoUnit.DAYS);
            datetime = datetime.with(ChronoField.NANO_OF_DAY, LocalTime.MIDNIGHT.toNanoOfDay());
            Assertions.assertEquals(datetime, foundOrder.getStartDate());
            datetime = datetime.plus(1, ChronoUnit.DAYS);
            Assertions.assertEquals(datetime, foundOrder.getEndDate());

            // should try to renew same order
            String orderId3 = createOrderFromSubscriptionCommand.createFromSubscription(subscriptionDto);
            Assertions.assertEquals(orderId2, orderId3, "Should try to renew same order since process has not run through");

        }

    }

    @Test
    @RunIfProfile(profile = "local")
    void createFromCancelledSubscription() {
        ResponseEntity<OrderAggregateDto> orderResponse = generateSubscriptionOrderData(1, 1L, Period.DAILY, 2, false);
        ResponseEntity<SubscriptionIdsDto> subscriptionIds = createSubscriptions(orderResponse);
        Optional<Order> order = orderRepository.findById(Objects.requireNonNull(orderResponse.getBody()).getOrder().getOrderId());
        Optional<Subscription> subscription = subscriptionRepository.findById(Objects.requireNonNull(subscriptionIds.getBody().getSubscriptionIds()).iterator().next());

        if (order.isPresent() && subscription.isPresent()) {
            foundOrder = order.get();
            foundSubscription = subscription.get();
            PaymentMessage paymentMessage = new PaymentMessage();
            paymentMessage.setPaymentPaidTimestamp(DateTimeUtil.getDateTime());

            orderService.setOrderStartAndEndDate(foundOrder, foundSubscription, paymentMessage);
            // set subscription end date into past
            foundSubscription.setEndDate(LocalDateTime.now().minusDays(10));
            foundSubscription.setStatus(SubscriptionStatus.CANCELLED);
            subscriptionRepository.save(foundSubscription);

            SubscriptionDto subscriptionDto = getSubscriptionQuery.mapToDto(foundSubscription);
            String orderId = createOrderFromSubscriptionCommand.createFromSubscription(subscriptionDto);
            Assertions.assertNull(orderId, "Should not renew cancelled subscription");
        }

    }

}
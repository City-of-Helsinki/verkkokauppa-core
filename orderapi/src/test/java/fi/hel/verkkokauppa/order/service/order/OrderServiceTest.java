package fi.hel.verkkokauppa.order.service.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.constants.PaymentGatewayEnum;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.SubscriptionController;
import fi.hel.verkkokauppa.order.api.admin.SubscriptionAdminController;
import fi.hel.verkkokauppa.order.api.data.FlowStepDto;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.OrderDto;
import fi.hel.verkkokauppa.order.api.data.OrderItemDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionIdsDto;
import fi.hel.verkkokauppa.order.logic.subscription.NextDateCalculator;
import fi.hel.verkkokauppa.order.model.FlowStep;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.model.renewal.SubscriptionRenewalRequest;
import fi.hel.verkkokauppa.order.model.subscription.Period;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionStatus;
import fi.hel.verkkokauppa.order.repository.jpa.FlowStepRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRenewalRequestRepository;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.order.service.renewal.SubscriptionRenewalService;
import fi.hel.verkkokauppa.order.service.subscription.SubscriptionService;
import fi.hel.verkkokauppa.order.test.utils.TestUtils;
import fi.hel.verkkokauppa.order.testing.annotations.RunIfProfile;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
        "merchant.service.url=http://localhost:8188"
})
@RunIfProfile(profile = "local")
class OrderServiceTest extends TestUtils {

    private Logger log = LoggerFactory.getLogger(OrderServiceTest.class);
    @Autowired
    private OrderService orderService;

    @Autowired
    private FlowStepService flowStepService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private FlowStepRepository flowStepRepository;

    @Autowired
    private NextDateCalculator nextDateCalculator;

    @Autowired
    private SubscriptionService subscriptionService;
    @Autowired
    private SubscriptionController subscriptionController;

    @Autowired
    private SubscriptionRenewalService subscriptionRenewalService;

    @Autowired
    private SubscriptionAdminController subscriptionAdminController;

    @Autowired
    private SubscriptionRenewalRequestRepository requestRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Order foundOrder;
    private Subscription foundSubscription;

    private List<Order> ordersToBeDeleted = new ArrayList();
    private List<FlowStep> flowStepsToBeDeleted = new ArrayList();

    @AfterEach
    public void tearDown() {
        try {
            orderRepository.deleteAll(ordersToBeDeleted);
            flowStepRepository.deleteAll(flowStepsToBeDeleted);
            ordersToBeDeleted = new ArrayList<>();
            flowStepsToBeDeleted = new ArrayList<>();
        } catch (Exception e) {
            log.info("delete error {}", e.toString());
        }

    }

    @Test
    @RunIfProfile(profile = "local")
    void setOrderStartAndEndDate() {
        ResponseEntity<OrderAggregateDto> orderResponse = generateSubscriptionOrderData(1, 1L, Period.DAILY, 2);
        ResponseEntity<SubscriptionIdsDto> subscriptionIds = createSubscriptions(orderResponse);
        Optional<Order> order = orderRepository.findById(Objects.requireNonNull(orderResponse.getBody()).getOrder().getOrderId());
        Optional<Subscription> subscription = subscriptionRepository.findById(Objects.requireNonNull(subscriptionIds.getBody().getSubscriptionIds()).iterator().next());
        PaymentMessage message = new PaymentMessage();
        String paymentPaidTimestamp = DateTimeUtil.getDateTime();
        message.setPaymentPaidTimestamp(paymentPaidTimestamp);
        if (order.isPresent() && subscription.isPresent()) {
            foundOrder = order.get();
            foundSubscription = subscription.get();
            orderService.setOrderStartAndEndDate(foundOrder, foundSubscription, message);

            Assertions.assertEquals(foundOrder.getStartDate(), DateTimeUtil.fromFormattedDateTimeString(paymentPaidTimestamp));
            // End date = startDate plus periodFrequency and period eq. daily/monthly/yearly.
            Assertions.assertEquals(foundOrder.getEndDate(), DateTimeUtil.fromFormattedDateTimeString(paymentPaidTimestamp).plus(1, ChronoUnit.DAYS));
        }
    }

    @Test
    @RunIfProfile(profile = "local")
    void cancelOrder() {
        ResponseEntity<OrderAggregateDto> orderResponse = generateSubscriptionOrderData(1, 1L, Period.DAILY, 2);
        ResponseEntity<SubscriptionIdsDto> subscriptionIds = createSubscriptions(orderResponse);
        Optional<Order> order = orderRepository.findById(Objects.requireNonNull(orderResponse.getBody()).getOrder().getOrderId());
        order.ifPresent(value -> orderService.cancel(value));
    }

    @Test
    @RunIfProfile(profile = "local")
    void createFromSubscriptionTested() {
        ResponseEntity<OrderAggregateDto> orderResponse = generateSubscriptionOrderData(1, 1L, Period.MONTHLY, 1);
        String order1Id = orderResponse.getBody().getOrder().getOrderId();
        Order order1 = orderService.findById(order1Id);

        Assertions.assertEquals(OrderType.SUBSCRIPTION, order1.getType());

        // Get orderItems
        List<OrderItem> orderItems = orderItemService.findByOrderId(order1Id);

        Assertions.assertEquals(1, orderItems.size());
        OrderItem orderItemOrder1 = orderItems.get(0);
        // Period 1 month from now 03.12.2021 - 03.01.2022
        Assertions.assertEquals(orderItemOrder1.getPeriodCount(), 1);
        Assertions.assertEquals(orderItemOrder1.getPeriodFrequency(), 1);
        Assertions.assertEquals(orderItemOrder1.getPeriodUnit(), Period.MONTHLY);

        // No payments yet
        Assertions.assertNull(order1.getEndDate());
        Assertions.assertNull(order1.getStartDate());

        // No subscriptions created yet for order
        Assertions.assertNull(order1.getSubscriptionId());

        LocalDateTime today = DateTimeUtil.getFormattedDateTime();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

        String todayDateAsString = today.format(formatter);
        Assertions.assertEquals(todayDateAsString, todayDateAsString);

        // FIRST payment today -> 03.12.2021
        PaymentMessage firstPayment = PaymentMessage
                .builder()
                .paymentId("1")
                .orderId(order1Id)
                .namespace(order1.getNamespace())
                .userId(order1.getUser())
                .orderType(order1.getType())
                .paymentPaidTimestamp(today.toString())
                .build();

        // Mimic first payment event
        subscriptionController.paymentPaidEventCallback(firstPayment);

        // Refetch order1 from db
        order1 = orderService.findById(order1Id);
        // FIRST Payment paid, period one month paid
        Assertions.assertEquals(todayDateAsString, order1.getStartDate().format(formatter));
        log.info("todayDateAsString {}", todayDateAsString);


        LocalDateTime minusOneDay = today
                .plus(1, ChronoUnit.MONTHS)
                .minus(1, ChronoUnit.DAYS);

        LocalDateTime real = minusOneDay.with(ChronoField.NANO_OF_DAY, LocalTime.MAX.toNanoOfDay());
        // End datetime: start datetime + 1 month - 1day(end of the day)
        String oneMonthFromTodayMinusOneDayEndOfThatDay = real
                .format(formatter);
        log.info("oneMonthFromTodayMinusOneDayEndOfThatDay {}", oneMonthFromTodayMinusOneDayEndOfThatDay);
        Assertions.assertEquals(oneMonthFromTodayMinusOneDayEndOfThatDay, order1.getEndDate().format(formatter));
        // Start date is payment paid timestamp
        Assertions.assertEquals(order1.getStartDate().format(formatter), todayDateAsString);

        LocalDateTime periodAdded = nextDateCalculator.calculateNextDateTime(
                today,
                orderItemOrder1.getPeriodUnit(),
                orderItemOrder1.getPeriodFrequency());
        LocalDateTime calculatedNewEndDate = nextDateCalculator.calculateNextEndDateTime(
                periodAdded,
                Period.MONTHLY
        );
        Assertions.assertEquals(calculatedNewEndDate.format(formatter),
                oneMonthFromTodayMinusOneDayEndOfThatDay);
        // Asserts that endDate is moved + 1 month from today date - 1 day (end of day).
        Assertions.assertEquals(oneMonthFromTodayMinusOneDayEndOfThatDay, order1.getEndDate().format(formatter));

        // Is subscription created
        Assertions.assertNotNull(order1.getSubscriptionId());


        String firstSubscriptionId = order1.getSubscriptionId();
        // Fetch subscription
        Subscription firstSubscription = subscriptionService.findById(firstSubscriptionId);

        Assertions.assertNull(firstSubscription.getPaymentGateway());

        // Checks merchant Id is added to subscription
        Assertions.assertNotNull(firstSubscription.getMerchantId());

        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("dd.MM.yyyy HH");

        // Assert dates matches
        Assertions.assertEquals(order1.getStartDate().format(formatter2), firstSubscription.getStartDate().format(formatter2));
        Assertions.assertEquals(order1.getEndDate().format(formatter), firstSubscription.getEndDate().format(formatter));
        log.info("order 1 startDate {}", order1.getStartDate().format(formatter));
        log.info("order 1 endDate {}", order1.getEndDate().format(formatter));
        log.info("subscription 1 startDate {}", firstSubscription.getStartDate().format(formatter));
        log.info("subscription 1 endDate {}", firstSubscription.getEndDate().format(formatter));

        // FIRST Payment paid, period one month paid
        Assertions.assertEquals(todayDateAsString, order1.getStartDate().format(formatter));
        Assertions.assertEquals(today.format(formatter2), firstSubscription.getStartDate().format(formatter2));
        // FIRST Payment paid, period one month paid
        Assertions.assertEquals(oneMonthFromTodayMinusOneDayEndOfThatDay, order1.getEndDate().format(formatter));
        Assertions.assertEquals(oneMonthFromTodayMinusOneDayEndOfThatDay, firstSubscription.getEndDate().format(formatter));

        // RENEWAL PROCESS START 1
        // There should be no need to renew this subscription yet

        // next renewal date should be 3 days from endDate (31.12.2021) -> threeDaysBeforeEndDate
        // cannot be tested

        // Renew subscription

        String order2FromSubscriptionId = subscriptionRenewalService.renewSubscription(firstSubscriptionId);
        subscriptionRenewalService.finishRenewingSubscription(firstSubscriptionId);
        // Fetch second subs
        Order order2 = orderService.findById(order2FromSubscriptionId);

        // Start datetime: Previous order enddate + 1 day(start of the day)
        LocalDateTime renewalStartDate = order2
                .getEndDate()
                .plus(1, ChronoUnit.DAYS)
                .minus(1, ChronoUnit.MONTHS)
                .with(ChronoField.NANO_OF_DAY, LocalTime.MIDNIGHT.toNanoOfDay());
        Assertions.assertEquals(renewalStartDate.format(formatter), order2.getStartDate().format(formatter));

        String twoMonthFromTodayMinusOneDayEndOfThatDay = today
                .plus(2, ChronoUnit.MONTHS)
                .minus(1, ChronoUnit.DAYS)
                .with(ChronoField.NANO_OF_DAY, LocalTime.MAX.toNanoOfDay())
                .format(formatter);
        Assertions.assertEquals(twoMonthFromTodayMinusOneDayEndOfThatDay, order2.getEndDate().format(formatter));
        // RENEWAL PROCESS END 1

        // RENEWAL PROCESS START 2
        log.info("order2.getStartDate() {}", order2.getStartDate().format(formatter));
        log.info("order2.getEndDate() {}", order2.getEndDate().format(formatter));

        // There should be no need to renew this subscription yet, has active subscription order
        String order3FromOrder = subscriptionRenewalService.renewSubscription(firstSubscriptionId);
        log.info("order2FromSubscriptionId {}", order2FromSubscriptionId);
        log.info("order3FromOrder {}", order3FromOrder);
        Assertions.assertEquals(order2FromSubscriptionId, order3FromOrder);
        // RENEWAL PROCESS END 2

        // RENEWAL PROCESS START 3
        // Update start and endDate of order programmatically
        firstSubscription.setStartDate(today.minus(2, ChronoUnit.MONTHS));
        firstSubscription.setEndDate(today.minus(1, ChronoUnit.MONTHS));
        subscriptionRepository.save(firstSubscription);
        Assertions.assertEquals(SubscriptionStatus.ACTIVE, firstSubscription.getStatus());
        subscriptionAdminController.checkRenewals();
        Optional<Subscription> refetchFirstSubscription = subscriptionRepository.findById(firstSubscription.getSubscriptionId());
        if (refetchFirstSubscription.isPresent()) {
            firstSubscription = refetchFirstSubscription.get();
            Assertions.assertEquals(SubscriptionStatus.CANCELLED, firstSubscription.getStatus());

            // Cancelled subscription should not be renewed
            Optional<SubscriptionRenewalRequest> getRenewalRequest = requestRepository.findById(firstSubscription.getSubscriptionId());
            Assertions.assertFalse(getRenewalRequest.isPresent());
        }
        // RENEWAL PROCESS END 3
    }

    @Test
    @RunIfProfile(profile = "local")
    void createFromSubscriptionDaily() {
        ResponseEntity<OrderAggregateDto> orderResponse = generateSubscriptionOrderData(1, 1L, Period.MONTHLY, 1);
        String order1Id = orderResponse.getBody().getOrder().getOrderId();
        Order order1 = orderService.findById(order1Id);

        Assertions.assertEquals(OrderType.SUBSCRIPTION, order1.getType());

        // Get orderItems
        List<OrderItem> orderItems = orderItemService.findByOrderId(order1Id);

        Assertions.assertEquals(1, orderItems.size());
        OrderItem orderItemOrder1 = orderItems.get(0);
        // Period 1 month from now 03.12.2021 - 03.01.2022
        Assertions.assertEquals(orderItemOrder1.getPeriodCount(), 1);
        Assertions.assertEquals(orderItemOrder1.getPeriodFrequency(), 1);
        Assertions.assertEquals(orderItemOrder1.getPeriodUnit(), Period.MONTHLY);

        // No payments yet
        Assertions.assertNull(order1.getEndDate());
        Assertions.assertNull(order1.getStartDate());

        // No subscriptions created yet for order
        Assertions.assertNull(order1.getSubscriptionId());

        LocalDateTime today = DateTimeUtil.getFormattedDateTime();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        String todayDateAsString = today.format(formatter);
        Assertions.assertEquals(todayDateAsString, todayDateAsString);

        // FIRST payment today -> 03.12.2021
        PaymentMessage firstPayment = PaymentMessage
                .builder()
                .paymentId("1")
                .orderId(order1Id)
                .namespace(order1.getNamespace())
                .userId(order1.getUser())
                .orderType(order1.getType())
                .paymentPaidTimestamp(today.toString())
                .build();

        // Mimic first payment event
        subscriptionController.paymentPaidEventCallback(firstPayment);

        // Refetch order1 from db
        order1 = orderService.findById(order1Id);
        // FIRST Payment paid, period one month paid
        Assertions.assertEquals(todayDateAsString, order1.getStartDate().format(formatter));
        log.info("todayDateAsString {}", todayDateAsString);
        String oneMonthFromMinusOneDayToday = today
                .plus(1, ChronoUnit.MONTHS)
                .minus(1, ChronoUnit.DAYS)
                .format(formatter);
        log.info("oneMonthFromMinusOneDayToday {}", oneMonthFromMinusOneDayToday);
        Assertions.assertEquals(oneMonthFromMinusOneDayToday, order1.getEndDate().format(formatter));
        // Start date is payment paid timestamp
        Assertions.assertEquals(order1.getStartDate().format(formatter), todayDateAsString);

        String oneMonthFromToday = today
                .plus(1, ChronoUnit.MONTHS)
                .format(formatter);

        Assertions.assertEquals(nextDateCalculator.calculateNextDateTime(
                        today,
                        orderItemOrder1.getPeriodUnit(),
                        orderItemOrder1.getPeriodFrequency()).format(formatter),
                oneMonthFromToday);
        // Asserts that endDate is moved + 1 month minus one day from today date.
        Assertions.assertEquals(oneMonthFromMinusOneDayToday, order1.getEndDate().format(formatter));

        // Is subscription created
        Assertions.assertNotNull(order1.getSubscriptionId());

        String firstSubscriptionId = order1.getSubscriptionId();
        // Fetch subscription
        Subscription firstSubscription = subscriptionService.findById(firstSubscriptionId);

        // Assert dates matches
        Assertions.assertEquals(order1.getStartDate().format(formatter), firstSubscription.getStartDate().format(formatter));
        Assertions.assertEquals(order1.getEndDate().format(formatter), firstSubscription.getEndDate().format(formatter));
        log.info("order 1 startDate {}", order1.getStartDate().format(formatter));
        log.info("order 1 endDate {}", order1.getEndDate().format(formatter));

        // FIRST Payment paid, period one month paid
        Assertions.assertEquals(todayDateAsString, order1.getStartDate().format(formatter));
        Assertions.assertEquals(todayDateAsString, firstSubscription.getStartDate().format(formatter));
        // FIRST Payment paid, period one month paid
        Assertions.assertEquals(oneMonthFromMinusOneDayToday, order1.getEndDate().format(formatter));
        Assertions.assertEquals(oneMonthFromMinusOneDayToday, firstSubscription.getEndDate().format(formatter));

        // RENEWAL PROCESS START 1
        // There should be no need to renew this subscription yet

        // next renewal date should be 3 days from endDate (31.12.2021) -> threeDaysBeforeEndDate
        // cannot be tested

        // Renew subscription

        String order2FromSubscriptionId = subscriptionRenewalService.renewSubscription(firstSubscriptionId);
        subscriptionRenewalService.finishRenewingSubscription(firstSubscriptionId);
        // Fetch second subs
        Order order2 = orderService.findById(order2FromSubscriptionId);

        Assertions.assertEquals(oneMonthFromToday, order2.getStartDate().format(formatter));

        String twoMonthFromTodayMinusOneDay = today
                .plus(2, ChronoUnit.MONTHS)
                .minus(1,ChronoUnit.DAYS)
                .format(formatter);
        Assertions.assertEquals(twoMonthFromTodayMinusOneDay, order2.getEndDate().format(formatter));
        // RENEWAL PROCESS END 1

        // RENEWAL PROCESS START 2
        log.info("order2.getStartDate() {}", order2.getStartDate().format(formatter));
        log.info("order2.getEndDate() {}", order2.getEndDate().format(formatter));

        // There should be no need to renew this subscription yet, has active subscription order
        String order3FromOrder = subscriptionRenewalService.renewSubscription(firstSubscriptionId);
        log.info("order2FromSubscriptionId {}", order2FromSubscriptionId);
        log.info("order3FromOrder {}", order3FromOrder);
        Assertions.assertEquals(order2FromSubscriptionId, order3FromOrder);
        // RENEWAL PROCESS END 2

        // RENEWAL PROCESS START 3
        // Update start and endDate of order programmatically
        firstSubscription.setStartDate(today.minus(2, ChronoUnit.MONTHS));
        firstSubscription.setEndDate(today.minus(1, ChronoUnit.MONTHS));
        subscriptionRepository.save(firstSubscription);
        Assertions.assertEquals(SubscriptionStatus.ACTIVE, firstSubscription.getStatus());
        subscriptionAdminController.checkRenewals();
        Optional<Subscription> refetchFirstSubscription = subscriptionRepository.findById(firstSubscription.getSubscriptionId());
        if (refetchFirstSubscription.isPresent()) {
            firstSubscription = refetchFirstSubscription.get();
            Assertions.assertEquals(SubscriptionStatus.CANCELLED, firstSubscription.getStatus());
        }
        // RENEWAL PROCESS END 3
    }

    @Test
    @RunIfProfile(profile = "local")
    void createFromSubscriptionAllowCurrentDayRenewalTested() {
        ResponseEntity<OrderAggregateDto> orderResponse = generateSubscriptionOrderData(1, 1L, Period.MONTHLY, 1);
        String order1Id = orderResponse.getBody().getOrder().getOrderId();
        Order order1 = orderService.findById(order1Id);

        Assertions.assertEquals(OrderType.SUBSCRIPTION, order1.getType());

        // Get orderItems
        List<OrderItem> orderItems = orderItemService.findByOrderId(order1Id);

        Assertions.assertEquals(1, orderItems.size());
        OrderItem orderItemOrder1 = orderItems.get(0);
        // Period 1 month from now 03.12.2021 - 03.01.2022
        Assertions.assertEquals(orderItemOrder1.getPeriodCount(), 1);
        Assertions.assertEquals(orderItemOrder1.getPeriodFrequency(), 1);
        Assertions.assertEquals(orderItemOrder1.getPeriodUnit(), Period.MONTHLY);

        // No payments yet
        Assertions.assertNull(order1.getEndDate());
        Assertions.assertNull(order1.getStartDate());

        // No subscriptions created yet for order
        Assertions.assertNull(order1.getSubscriptionId());

        LocalDateTime today = DateTimeUtil.getFormattedDateTime();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

        String todayDateAsString = today.format(formatter);
        Assertions.assertEquals(todayDateAsString, todayDateAsString);

        // FIRST payment today -> 03.12.2021
        PaymentMessage firstPayment = PaymentMessage
                .builder()
                .paymentId("1")
                .orderId(order1Id)
                .namespace(order1.getNamespace())
                .userId(order1.getUser())
                .orderType(order1.getType())
                .paymentPaidTimestamp(today.toString())
                .build();

        // Mimic first payment event
        subscriptionController.paymentPaidEventCallback(firstPayment);

        // Refetch order1 from db
        order1 = orderService.findById(order1Id);
        // FIRST Payment paid, period one month paid
        Assertions.assertEquals(todayDateAsString, order1.getStartDate().format(formatter));
        log.info("todayDateAsString {}", todayDateAsString);


        LocalDateTime minusOneDay = today
                .plus(1, ChronoUnit.MONTHS)
                .minus(1, ChronoUnit.DAYS);

        LocalDateTime real = minusOneDay.with(ChronoField.NANO_OF_DAY, LocalTime.MAX.toNanoOfDay());
        // End datetime: start datetime + 1 month - 1day(end of the day)
        String oneMonthFromTodayMinusOneDayEndOfThatDay = real
                .format(formatter);
        log.info("oneMonthFromTodayMinusOneDayEndOfThatDay {}", oneMonthFromTodayMinusOneDayEndOfThatDay);
        Assertions.assertEquals(oneMonthFromTodayMinusOneDayEndOfThatDay, order1.getEndDate().format(formatter));
        // Start date is payment paid timestamp
        Assertions.assertEquals(order1.getStartDate().format(formatter), todayDateAsString);

        LocalDateTime periodAdded = nextDateCalculator.calculateNextDateTime(
                today,
                orderItemOrder1.getPeriodUnit(),
                orderItemOrder1.getPeriodFrequency());
        LocalDateTime calculatedNewEndDate = nextDateCalculator.calculateNextEndDateTime(
                periodAdded,
                Period.MONTHLY
        );
        Assertions.assertEquals(calculatedNewEndDate.format(formatter),
                oneMonthFromTodayMinusOneDayEndOfThatDay);
        // Asserts that endDate is moved + 1 month from today date - 1 day (end of day).
        Assertions.assertEquals(oneMonthFromTodayMinusOneDayEndOfThatDay, order1.getEndDate().format(formatter));

        // Is subscription created
        Assertions.assertNotNull(order1.getSubscriptionId());

        String firstSubscriptionId = order1.getSubscriptionId();
        // Fetch subscription
        Subscription firstSubscription = subscriptionService.findById(firstSubscriptionId);

        DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("dd.MM.yyyy HH");

        // Assert dates matches
        Assertions.assertEquals(order1.getStartDate().format(formatter2), firstSubscription.getStartDate().format(formatter2));
        Assertions.assertEquals(order1.getEndDate().format(formatter), firstSubscription.getEndDate().format(formatter));
        log.info("order 1 startDate {}", order1.getStartDate().format(formatter));
        log.info("order 1 endDate {}", order1.getEndDate().format(formatter));
        log.info("subscription 1 startDate {}", firstSubscription.getStartDate().format(formatter));
        log.info("subscription 1 endDate {}", firstSubscription.getEndDate().format(formatter));

        // FIRST Payment paid, period one month paid
        Assertions.assertEquals(todayDateAsString, order1.getStartDate().format(formatter));
        Assertions.assertEquals(today.format(formatter2), firstSubscription.getStartDate().format(formatter2));
        // FIRST Payment paid, period one month paid
        Assertions.assertEquals(oneMonthFromTodayMinusOneDayEndOfThatDay, order1.getEndDate().format(formatter));
        Assertions.assertEquals(oneMonthFromTodayMinusOneDayEndOfThatDay, firstSubscription.getEndDate().format(formatter));

        // RENEWAL PROCESS START 1
        // There should be no need to renew this subscription yet

        // next renewal date should be 3 days from endDate (31.12.2021) -> threeDaysBeforeEndDate
        // cannot be tested

        // Renew subscription

        String order2FromSubscriptionId = subscriptionRenewalService.renewSubscription(firstSubscriptionId);
        subscriptionRenewalService.finishRenewingSubscription(firstSubscriptionId);
        // Fetch second subs
        Order order2 = orderService.findById(order2FromSubscriptionId);

        // Start datetime: Previous order enddate + 1 day(start of the day)
        LocalDateTime renewalStartDate = order2
                .getEndDate()
                .plus(1, ChronoUnit.DAYS)
                .minus(1, ChronoUnit.MONTHS)
                .with(ChronoField.NANO_OF_DAY, LocalTime.MIDNIGHT.toNanoOfDay());
        Assertions.assertEquals(renewalStartDate.format(formatter), order2.getStartDate().format(formatter));

        String twoMonthFromTodayMinusOneDayEndOfThatDay = today
                .plus(2, ChronoUnit.MONTHS)
                .minus(1, ChronoUnit.DAYS)
                .with(ChronoField.NANO_OF_DAY, LocalTime.MAX.toNanoOfDay())
                .format(formatter);
        Assertions.assertEquals(twoMonthFromTodayMinusOneDayEndOfThatDay, order2.getEndDate().format(formatter));
        // RENEWAL PROCESS END 1

        // RENEWAL PROCESS START 2
        log.info("order2.getStartDate() {}", order2.getStartDate().format(formatter));
        log.info("order2.getEndDate() {}", order2.getEndDate().format(formatter));

        // RENEWAL PROCESS START 3 (KYV-505)
        // Update start and endDate of order programmatically
        firstSubscription.setStartDate(today.minus(2, ChronoUnit.MONTHS));
        // Set endDate at the start of the day, '00:00'.
        firstSubscription.setEndDate(
                today.with(ChronoField.NANO_OF_DAY, LocalTime.MIDNIGHT.toNanoOfDay())
        );
        subscriptionRepository.save(firstSubscription);
        Assertions.assertEquals(SubscriptionStatus.ACTIVE, firstSubscription.getStatus());
        subscriptionAdminController.checkRenewals();
        Optional<Subscription> refetchFirstSubscription = subscriptionRepository.findById(firstSubscription.getSubscriptionId());
        if (refetchFirstSubscription.isPresent()) {
            firstSubscription = refetchFirstSubscription.get();
            Assertions.assertEquals(SubscriptionStatus.ACTIVE, firstSubscription.getStatus());
        }
        // RENEWAL PROCESS END 3

        // RENEWAL PROCESS START 4 (Automatically cancels subscription when endDate is today -1 day)
        // Update start and endDate of order programmatically
        firstSubscription.setStartDate(today.minus(2, ChronoUnit.MONTHS));
        // Set endDate at the start of the day, '00:00'.
        firstSubscription.setEndDate(
                today
                        .minus(1, ChronoUnit.DAYS)
                        .with(ChronoField.NANO_OF_DAY, LocalTime.MAX.toNanoOfDay())
        );
        subscriptionRepository.save(firstSubscription);
        Assertions.assertEquals(SubscriptionStatus.ACTIVE, firstSubscription.getStatus());
        subscriptionAdminController.checkRenewals();
        Optional<Subscription> refetchFirstSubscription2 = subscriptionRepository.findById(firstSubscription.getSubscriptionId());
        if (refetchFirstSubscription2.isPresent()) {
            firstSubscription = refetchFirstSubscription2.get();
            Assertions.assertEquals(SubscriptionStatus.CANCELLED, firstSubscription.getStatus());
        }
        // RENEWAL PROCESS END 4
    }

    @Test
    @RunIfProfile(profile = "local")
    void createOrderWithMerchantId() throws JsonProcessingException {
        String namespace = "venepaikat";
        // Helper test function to create new order with merchantId in orderItems, if initialization is done to merchants/namespace.
        String firstMerchantIdFromNamespace = getFirstMerchantIdFromNamespace(namespace);
        log.info("Creating order with merchantId: {}",firstMerchantIdFromNamespace);
        OrderAggregateDto createOrderResponse = createNewOrderToDatabase(1, firstMerchantIdFromNamespace, namespace, "8a8674ed-1ae2-3ca9-a93c-036478b2a032").getBody();
        log.info(objectMapper.writeValueAsString(createOrderResponse));
        assert createOrderResponse != null;
        Order order = orderRepository.findById(createOrderResponse.getOrder().getOrderId()).get();
        OrderItemDto orderItem = createOrderResponse.getItems().get(0);

        log.info("Created order with orderId: {}", order.getOrderId());
        log.info("Created order with userId: {}", order.getUser());
        log.info("Created order with merchantId: {}", firstMerchantIdFromNamespace);
        log.info("Kassa URL: {}", "https://localhost:3000/" + order.getOrderId() + "?user=" + order.getUser());
        order.setPriceNet(String.valueOf(new BigDecimal(orderItem.getPriceNet())));
        order.setPriceVat(String.valueOf(new BigDecimal(orderItem.getPriceVat())));
        order.setPriceTotal(String.valueOf(new BigDecimal(orderItem.getRowPriceTotal())));
        Assertions.assertEquals(firstMerchantIdFromNamespace,orderItem.getMerchantId());

        orderRepository.save(order);
    }

    @Test
    @RunIfProfile(profile = "local")
    void createOrderWithFeeItemAndMerchantId() throws JsonProcessingException {
        // Helper test function to create new order with merchantId in orderItems, if initialization is done to merchants/namespace.
        String firstMerchantIdFromNamespace = getFirstMerchantIdFromNamespace("venepaikat");
        log.info("Creating order with merchantId: {}",firstMerchantIdFromNamespace);
        OrderAggregateDto createOrderResponse = createNewOrderWithFreeItemToDatabase(1, firstMerchantIdFromNamespace).getBody();
        log.info(objectMapper.writeValueAsString(createOrderResponse));
        assert createOrderResponse != null;
        Order order = orderRepository.findById(createOrderResponse.getOrder().getOrderId()).get();
        OrderItemDto orderItem = createOrderResponse.getItems().get(0);

        log.info("Created order with orderId: {}", order.getOrderId());
        log.info("Created order with userId: {}", order.getUser());
        log.info("Created order with merchantId: {}", firstMerchantIdFromNamespace);
        log.info("Kassa URL: {}", "https://localhost:3000/" + order.getOrderId() + "?user=" + order.getUser());
        order.setPriceNet(String.valueOf(new BigDecimal(orderItem.getPriceNet())));
        order.setPriceVat(String.valueOf(new BigDecimal(orderItem.getPriceVat())));
        order.setPriceTotal(String.valueOf(new BigDecimal(orderItem.getRowPriceTotal())));
        Assertions.assertEquals(firstMerchantIdFromNamespace,orderItem.getMerchantId());

        orderRepository.save(order);
    }

    @Test
    @RunIfProfile(profile = "local")
    void createFreeOrderWithMerchantId() throws JsonProcessingException {
        // Helper test function to create new order with merchantId in orderItems, if initialization is done to merchants/namespace.
        String firstMerchantIdFromNamespace = getFirstMerchantIdFromNamespace("venepaikat");
        log.info("Creating order with merchantId: {}",firstMerchantIdFromNamespace);
        OrderAggregateDto createOrderResponse = createNewFreeOrderToDatabase(1, firstMerchantIdFromNamespace).getBody();
        log.info(objectMapper.writeValueAsString(createOrderResponse));
        assert createOrderResponse != null;
        Order order = orderRepository.findById(createOrderResponse.getOrder().getOrderId()).get();
        OrderItemDto orderItem = createOrderResponse.getItems().get(0);

        log.info("Created order with orderId: {}", order.getOrderId());
        log.info("Created order with userId: {}", order.getUser());
        log.info("Created order with merchantId: {}", firstMerchantIdFromNamespace);
        log.info("Kassa URL: {}", "https://localhost:3000/" + order.getOrderId() + "?user=" + order.getUser());
        order.setPriceNet(String.valueOf(new BigDecimal(orderItem.getPriceNet())));
        order.setPriceVat(String.valueOf(new BigDecimal(orderItem.getPriceVat())));
        order.setPriceTotal(String.valueOf(new BigDecimal(orderItem.getRowPriceTotal())));
        Assertions.assertEquals(firstMerchantIdFromNamespace,orderItem.getMerchantId());

        orderRepository.save(order);
    }

    @Test
    @RunIfProfile(profile = "local")
    void createInvoiceTypeOrderWithMerchantId() throws JsonProcessingException {
        // Helper test function to create new order with merchantId in orderItems, if initialization is done to merchants/namespace.
        String firstMerchantIdFromNamespace = getFirstMerchantIdFromNamespace("venepaikat");
        log.info("Creating invoice type order with merchantId: {}",firstMerchantIdFromNamespace);
        OrderAggregateDto createOrderResponse = createNewOrderToDatabase(1, firstMerchantIdFromNamespace).getBody();
        log.info(objectMapper.writeValueAsString(createOrderResponse));
        assert createOrderResponse != null;
        Order order = orderRepository.findById(createOrderResponse.getOrder().getOrderId()).get();
        OrderItem orderItemModel = orderItemService.findByOrderId(order.getOrderId()).get(0);
        OrderItemDto orderItem = createOrderResponse.getItems().get(0);

        order.setPriceNet(String.valueOf(new BigDecimal(orderItem.getPriceNet())));
        order.setPriceVat(String.valueOf(new BigDecimal(orderItem.getPriceVat())));
        order.setPriceTotal(String.valueOf(new BigDecimal(orderItem.getRowPriceTotal())));

        Assertions.assertEquals(firstMerchantIdFromNamespace,orderItem.getMerchantId());
        orderRepository.save(order);

        // Create invoicing accountings
        createMockInvoiceAccountingForProductId(orderItem.getProductId());
        JSONObject response = createMockAccountingForProductId(orderItem.getProductId());
        orderItemModel.setInvoicingDate(LocalDate.now());
        orderItemRepository.save(orderItemModel);
        log.info("Created order with orderId: {}", order.getOrderId());
        log.info("Created order with userId: {}", order.getUser());
        log.info("Created order with merchantId: {}", firstMerchantIdFromNamespace);
        log.info("Kassa URL: {}", "https://localhost:3000/" + order.getOrderId() + "?user=" + order.getUser());
    }

    @Test
    @RunIfProfile(profile = "local")
    void createSubsriptionOrderWithMerchantId() throws JsonProcessingException {
        // Helper test function to create new order with merchantId in orderItems, if initialization is done to merchants/namespace.
        String firstMerchantIdFromNamespace = getFirstMerchantIdFromNamespace("venepaikat");

        // setup product for test
        String productId = createMockProductMapping("venepaikat",firstMerchantIdFromNamespace);
        JSONObject response = createMockAccountingForProductId(productId);

        ResponseEntity<OrderAggregateDto> orderResponse = generateSubscriptionOrderData(1, 1L, Period.DAILY, 2, true, productId);
        OrderDto order = Objects.requireNonNull(orderResponse.getBody()).getOrder();
        log.info("Created order with merchantId: {}", firstMerchantIdFromNamespace);
        log.info("Kassa URL: {}", "https://localhost:3000/" + order.getOrderId() + "?user=" + order.getUser());

        Order order1 = orderRepository.findById(order.getOrderId()).orElse(null);
        assert order1 != null;
        OrderItemDto orderItem = orderResponse.getBody().getItems().get(0);
        String price = "100";
        order1.setPriceNet(String.valueOf(new BigDecimal(price)));
        order1.setPriceVat(String.valueOf(new BigDecimal(price)));
        order1.setPriceTotal(String.valueOf(new BigDecimal(price)));
        Assertions.assertEquals(firstMerchantIdFromNamespace,orderItem.getMerchantId());


        orderRepository.save(order1);
    }

    @Test
    @RunIfProfile(profile = "local")
    public void createOrderWithLastValidPurchaseDateTime() {
        String firstMerchantIdFromNamespace = getFirstMerchantIdFromNamespace("venepaikat");
        OrderAggregateDto createOrderResponse = createNewOrderToDatabase(1, firstMerchantIdFromNamespace).getBody();
        assert createOrderResponse != null;
        Order order = orderRepository.findById(createOrderResponse.getOrder().getOrderId()).get();
        LocalDateTime localDateTime = LocalDateTime.now();
        order.setLastValidPurchaseDateTime(localDateTime);
        orderRepository.save(order);
        Order fetchedOrder = orderRepository.findById(order.getOrderId()).get();

        /* Creating localDateTime with now() uses 6 digit precision with nanoseconds by default.
           This is automatically rounded on save on db's side to 3 digit precision which causes assertion
           error if values are asserted as is. Creating a formatter just for this felt excessive, thus
           nanoseconds are rounded to 0 for "good-enough" assertion. */
        Assertions.assertEquals(
                localDateTime.withNano(0),
                fetchedOrder.getLastValidPurchaseDateTime().withNano(0)
        );
        ordersToBeDeleted.add(fetchedOrder);
    }
    @Test
    @RunIfProfile(profile = "local")
    public void createOrderByParams() {
        String namespace = "venepaikat";
        String user = "dummy_user";
        LocalDateTime localDateTime = LocalDateTime.now();

        Order order = orderService.createByParams(namespace, user, localDateTime);
        Assertions.assertEquals(namespace, order.getNamespace());
        Assertions.assertEquals(user, order.getUser());
        Assertions.assertEquals(
                localDateTime,
                order.getLastValidPurchaseDateTime()
        );

        Order fetchedOrder = orderRepository.findById(order.getOrderId()).get();
        Assertions.assertEquals(namespace, fetchedOrder.getNamespace());
        Assertions.assertEquals(user, fetchedOrder.getUser());
        Assertions.assertEquals(
                localDateTime.withNano(0),
                fetchedOrder.getLastValidPurchaseDateTime().withNano(0)
        );
        ordersToBeDeleted.add(fetchedOrder);
    }

    @Test
    @RunIfProfile(profile = "local")
    public void createOrderByParamsWithNullDateTime() {
        String namespace = "venepaikat";
        String user = "dummy_user";
        LocalDateTime localDateTime = null;

        Order order = orderService.createByParams(namespace, user, localDateTime);
        Assertions.assertEquals(namespace, order.getNamespace());
        Assertions.assertEquals(user, order.getUser());
        Assertions.assertNull(order.getLastValidPurchaseDateTime());

        Order fetchedOrder = orderRepository.findById(order.getOrderId()).get();
        Assertions.assertEquals(namespace, fetchedOrder.getNamespace());
        Assertions.assertEquals(user, fetchedOrder.getUser());
        Assertions.assertNull(order.getLastValidPurchaseDateTime());
        ordersToBeDeleted.add(fetchedOrder);
    }

    @Test
    @RunIfProfile(profile = "local")
    public void createFlowStepsByOrderId() {
        String namespace = "venepaikat";
        String user = "dummy_user";
        LocalDateTime localDateTime = null;

        Order order = orderService.createByParams(namespace, user, localDateTime);
        Order fetchedOrder = orderRepository.findById(order.getOrderId()).get();

        FlowStepDto flowStepsDto = new FlowStepDto();
        flowStepsDto.setActiveStep(2);
        flowStepsDto.setTotalSteps(5);
        FlowStepDto savedFlowSteps = flowStepService.saveFlowStepsByOrderId(fetchedOrder.getOrderId(), flowStepsDto);

        Assertions.assertNotNull(savedFlowSteps.getFlowStepId());
        Assertions.assertEquals(fetchedOrder.getOrderId(), savedFlowSteps.getOrderId());
        Assertions.assertEquals(2, savedFlowSteps.getActiveStep());
        Assertions.assertEquals(5, savedFlowSteps.getTotalSteps());

        ordersToBeDeleted.add(fetchedOrder);
        flowStepsToBeDeleted.add(flowStepService.getFlowStepsByOrderId(fetchedOrder.getOrderId()).get());
    }

    @Test
    @RunIfProfile(profile = "local")
    public void createFlowStepsByOrderIdThatDoesNotExist() {
        FlowStepDto flowStepsDto = new FlowStepDto();
        flowStepsDto.setActiveStep(2);
        flowStepsDto.setTotalSteps(5);

        String orderIdNotExist = UUID.randomUUID().toString();

        CommonApiException exception = assertThrows(CommonApiException.class, () -> {
            flowStepService.saveFlowStepsByOrderId(orderIdNotExist, flowStepsDto);
        });

        assertEquals(CommonApiException.class, exception.getClass());
        assertEquals("order-not-found", exception.getErrors().getErrors().get(0).getCode());
        assertEquals("order with id [" + orderIdNotExist + "] not found", exception.getErrors().getErrors().get(0).getMessage());
    }

    @Test
    @RunIfProfile(profile = "local")
    void testSubscriptionPaytrailGateway() {
        Order order1 = orderService.findById(
            generateSubscriptionOrderData(1, 1L, Period.DAILY, 2).getBody().getOrder().getOrderId()
        );
        SubscriptionIdsDto subscriptionIds = subscriptionController.paymentPaidEventCallback(
            PaymentMessage
                    .builder()
                    .paymentId("1")
                    .orderId(order1.getOrderId())
                    .namespace(order1.getNamespace())
                    .userId(order1.getUser())
                    .orderType(order1.getType())
                    .paymentPaidTimestamp(DateTimeUtil.getDateTime())
                    .paymentGateway(PaymentGatewayEnum.PAYTRAIL)
                    .build()
        ).getBody();

        Assertions.assertEquals(subscriptionIds.getSubscriptionIds().size(), 1);

        Subscription subscription = subscriptionService.findById(subscriptionIds.getSubscriptionIds().iterator().next());

        Assertions.assertEquals(subscription.getPaymentGateway(), PaymentGatewayEnum.PAYTRAIL);

        subscriptionRenewalService.renewSubscription(subscription.getSubscriptionId());
    }
}

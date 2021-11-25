package fi.hel.verkkokauppa.order.service.order;

import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.admin.SubscriptionAdminController;
import fi.hel.verkkokauppa.order.api.SubscriptionController;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionIdsDto;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.subscription.Period;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.order.service.renewal.SubscriptionRenewalService;
import fi.hel.verkkokauppa.order.service.subscription.CreateOrderFromSubscriptionCommand;
import fi.hel.verkkokauppa.order.service.subscription.CreateSubscriptionsFromOrderCommand;
import fi.hel.verkkokauppa.order.service.subscription.GetSubscriptionQuery;
import fi.hel.verkkokauppa.order.service.subscription.SubscriptionService;
import fi.hel.verkkokauppa.order.test.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.doReturn;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
class OrderServiceTest extends TestUtils {

    private Logger log = LoggerFactory.getLogger(OrderServiceTest.class);
    @Autowired
    private OrderService orderService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SubscriptionService subscriptionService;
    @Autowired
    private SubscriptionController subscriptionController;

    @Autowired
    private CreateOrderFromSubscriptionCommand createOrderFromSubscriptionCommand;

    @Autowired
    private GetSubscriptionQuery getSubscriptionQuery;

    @Autowired
    private SubscriptionRenewalService subscriptionRenewalService;
    @Autowired
    private SubscriptionAdminController subscriptionAdminController;

    private Order foundOrder;
    private Subscription foundSubscription;

    @Test
    public void assertTrue() {
        Assertions.assertTrue(true);
    }

    @Test
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

            Assertions.assertEquals(foundOrder.getStartDate(), DateTimeUtil.fromFormattedString(paymentPaidTimestamp));
            // End date = startDate plus periodFrequency and period eq. daily/monthly/yearly.
            Assertions.assertEquals(foundOrder.getEndDate(), DateTimeUtil.fromFormattedString(paymentPaidTimestamp).plus(1, ChronoUnit.DAYS));
        }
    }

    @Test
    void createFromSubscription() {
        ResponseEntity<OrderAggregateDto> orderResponse = generateSubscriptionOrderData(1, 1L, Period.DAILY, 2);
        ResponseEntity<SubscriptionIdsDto> subscriptionIds = createSubscriptions(orderResponse);
        Optional<Order> order = orderRepository.findById(Objects.requireNonNull(orderResponse.getBody()).getOrder().getOrderId());
        Optional<Subscription> subscription = subscriptionRepository.findById(Objects.requireNonNull(subscriptionIds.getBody().getSubscriptionIds()).iterator().next());
        PaymentMessage message = new PaymentMessage();

        String paymentPaidTimestamp = DateTimeUtil.getDateTime();
        // 24.11.2021
        message.setPaymentPaidTimestamp(paymentPaidTimestamp);
        if (order.isPresent() && subscription.isPresent()) {

            foundOrder = order.get();
            foundSubscription = subscription.get();
            message.setOrderId(foundOrder.getOrderId());
            message.setUserId(foundOrder.getUser());
            // PAYMENT PAID
            // startDate 24.11.2021 and endDate 25.11.2021
            subscriptionService.afterFirstPaymentPaidEventActions(Set.of(foundSubscription.getSubscriptionId()), message);
            // PAYMENT PAID
            // Same end dates

            String noUpdate = subscriptionRenewalService.renewSubscription(foundSubscription.getSubscriptionId());
            Assertions.assertEquals(noUpdate, foundOrder.getOrderId(), "No renewal if has active order");

            // order endDate greater than current subscription endDate (isAfter)
            // OR subscriptionEndDate = last order endDate
            // startDate 21.11.2021 and endDate 22.11.2021
            orderService.setStartDateAndCalculateNextEndDate(foundOrder, foundSubscription, DateTimeUtil.getFormattedDateTime().minus(3, ChronoUnit.DAYS));
            orderRepository.save(foundOrder);
            // startDate 21.11.2021 and endDate 22.11.2021

            String created = subscriptionRenewalService.renewSubscription(foundSubscription.getSubscriptionId());

            foundSubscription = subscriptionRepository.findById(foundSubscription.getSubscriptionId()).get();
            Assertions.assertFalse(createOrderFromSubscriptionCommand.hasActiveSubscriptionOrder(foundSubscription, foundOrder));
            Assertions.assertNotEquals(created, foundOrder.getOrderId(), "Subscription renewal does not renew subscription if payment is paid");

            //TODO startDate 21.11.2021 and endDate 22.11.2021
            log.info(foundSubscription.getSubscriptionId());
            foundSubscription = subscriptionRepository.findById(foundSubscription.getSubscriptionId()).get();
            Order createdOrderFromSubscription = orderService.findById(created);
            // TODO Should these have some values or not?

            // Tries again -> Should return active order
            String created2 = subscriptionRenewalService.renewSubscription(foundSubscription.getSubscriptionId());
            Assertions.assertEquals(created, created2);

            // Tries again -> Should return active order
            String created3 = subscriptionRenewalService.renewSubscription(foundSubscription.getSubscriptionId());
            Assertions.assertEquals(created2, created3);
            // Update fields
            foundSubscription = getSubscriptionQuery.findByIdValidateByUser(foundSubscription.getSubscriptionId(),foundSubscription.getUser());
            foundOrder = orderService.findById(foundOrder.getOrderId());

            Order lastOrder = orderService.getLatestOrderWithSubscriptionId(foundSubscription.getSubscriptionId());
            Assertions.assertTrue(createOrderFromSubscriptionCommand.hasActiveSubscriptionOrder(foundSubscription, lastOrder));
        }
    }

}
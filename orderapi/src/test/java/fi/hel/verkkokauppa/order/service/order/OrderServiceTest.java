package fi.hel.verkkokauppa.order.service.order;

import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.SubscriptionAdminController;
import fi.hel.verkkokauppa.order.api.SubscriptionController;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionIdsDto;
import fi.hel.verkkokauppa.order.logic.subscription.NextDateCalculator;
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
import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
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
    private CreateSubscriptionsFromOrderCommand createSubscriptionsFromOrderCommand;

    @Autowired
    private GetSubscriptionQuery getSubscriptionQuery;

    @Autowired
    private SubscriptionRenewalService subscriptionRenewalService;
    @Autowired
    private SubscriptionAdminController subscriptionAdminController;

    private Order foundOrder;
    private Subscription foundSubscription;

    @Test
    public void assertTrue(){
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
        message.setPaymentPaidTimestamp(paymentPaidTimestamp);
        if (order.isPresent() && subscription.isPresent()) {
            foundOrder = order.get();
            foundSubscription = subscription.get();
            orderService.setOrderStartAndEndDate(foundOrder, foundSubscription, message);

            Assertions.assertEquals(foundOrder.getStartDate(), DateTimeUtil.fromFormattedString(paymentPaidTimestamp));
            // End date = startDate plus periodFrequency and period eq. daily/monthly/yearly.
            Assertions.assertEquals(foundOrder.getEndDate(), DateTimeUtil.fromFormattedString(paymentPaidTimestamp).plus(1, ChronoUnit.DAYS));

            subscriptionService.setSubscriptionEndDateFromOrder(foundOrder, foundSubscription);

            String noUpdate = subscriptionRenewalService.renewSubscription(foundSubscription.getSubscriptionId());
            Assertions.assertEquals(noUpdate, foundOrder.getOrderId(),"Subscription renewal does not renew subscription if no payment paid");

            // set orderEndDate is -1 days from paymentPaidTimestamp
            message.setPaymentPaidTimestamp(DateTimeUtil.fromFormattedString(paymentPaidTimestamp).minus(4, ChronoUnit.DAYS).toString());
            foundSubscription.setEndDate(DateTimeUtil.fromFormattedString(paymentPaidTimestamp).minus(4, ChronoUnit.DAYS));
            orderService.setOrderStartAndEndDate(foundOrder,foundSubscription,message);

            String created = subscriptionRenewalService.renewSubscription(foundSubscription.getSubscriptionId());
            Assertions.assertNotEquals(created, foundOrder.getOrderId(),"Subscription renewal does not renew subscription if payment is paid");

            log.info(foundSubscription.getSubscriptionId());

            Order createdOrderFromSubscription = orderService.findById(created);
            // TODO Should these have some values or not?
            Assertions.assertNull(createdOrderFromSubscription.getEndDate());
            Assertions.assertNull(createdOrderFromSubscription.getStartDate());

            List<SubscriptionDto> isFound = subscriptionAdminController
                    .getRenewableSubscriptions()
                    .stream()
                    .filter(subscriptionDto ->
                            Objects.equals(subscriptionDto.getSubscriptionId(), created))
                    .collect(Collectors.toList());
            Assertions.assertEquals(0, isFound.size());
//            OrderAggregateDto orderAggregateDto = orderService.getOrderWithItems(created);
//            Set<String> subscriptionsFromOrder = createSubscriptionsFromOrderCommand.createFromOrder(orderAggregateDto);
//            Assertions.assertEquals(1, subscriptionsFromOrder.size());
        }
    }

}
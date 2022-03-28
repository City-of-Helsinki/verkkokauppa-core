package fi.hel.verkkokauppa.order.api.admin;

import fi.hel.verkkokauppa.order.api.OrderController;
import fi.hel.verkkokauppa.order.api.SubscriptionController;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.logic.subscription.NextDateCalculator;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionStatus;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.order.service.order.OrderItemService;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import fi.hel.verkkokauppa.order.service.renewal.SubscriptionRenewalService;
import fi.hel.verkkokauppa.order.service.subscription.CreateOrderFromSubscriptionCommand;
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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
class SubscriptionAdminControllerTest extends TestUtils {

    private Logger log = LoggerFactory.getLogger(SubscriptionAdminControllerTest.class);
    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderController orderController;
    @Autowired
    private NextDateCalculator nextDateCalculator;

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
    void isExpiringSubscriptionCard() {
        LocalDate today = LocalDate.now();
        // Fetch subscription
        Subscription firstSubscription = createAndGetMonthlySubscription();
        SubscriptionDto subscriptionDto = subscriptionAdminController
                .getSubscription(firstSubscription.getSubscriptionId())
                .getBody();

        assert subscriptionDto != null;
        // False because subscription card information does not exists
        Assertions.assertFalse(subscriptionService.isExpiringCard(LocalDate.now(), subscriptionDto));

        // Add correct card information
        firstSubscription.setPaymentMethodExpirationMonth((byte) today.getMonth().getValue());
        firstSubscription.setPaymentMethodExpirationYear((short) today.getYear());

        subscriptionRepository.save(firstSubscription);

        SubscriptionDto refetchedDto = subscriptionAdminController
                .getSubscription(firstSubscription.getSubscriptionId())
                .getBody();
        assert refetchedDto != null;
        Assertions.assertFalse(subscriptionService.isExpiringCard(LocalDate.now(), refetchedDto));

        // Add expiring card information
        LocalDate todayMinusOneMonth = today.minus(1, ChronoUnit.MONTHS);
        firstSubscription.setPaymentMethodExpirationMonth((byte) todayMinusOneMonth.getMonth().getValue());
        firstSubscription.setPaymentMethodExpirationYear((short) todayMinusOneMonth.getYear());

        subscriptionRepository.save(firstSubscription);
        SubscriptionDto expiredCardSubscriptionDto = subscriptionAdminController
                .getSubscription(firstSubscription.getSubscriptionId())
                .getBody();
        assert expiredCardSubscriptionDto != null;
        Assertions.assertTrue(subscriptionService.isExpiringCard(LocalDate.now(), expiredCardSubscriptionDto));

        firstSubscription.setEndDate(LocalDateTime.now().minus(1, ChronoUnit.MONTHS));
        subscriptionRepository.save(firstSubscription);
        Assertions.assertEquals(SubscriptionStatus.ACTIVE, firstSubscription.getStatus());

    }

    @Test
    void isExpiringSubscriptionCardEndpoint() {
        LocalDate today = LocalDate.now();
        // Fetch subscription
        Subscription firstSubscription = createAndGetMonthlySubscription();
        SubscriptionDto subscriptionDto = subscriptionAdminController
                .getSubscription(firstSubscription.getSubscriptionId())
                .getBody();

        assert subscriptionDto != null;
        // False because subscription card information does not exists
        Assertions.assertFalse(subscriptionService.isExpiringCard(LocalDate.now(), subscriptionDto));

        // Add expiring card information
        LocalDate todayMinusOneMonth = today.minus(1, ChronoUnit.MONTHS);
        firstSubscription.setPaymentMethodExpirationMonth((byte) todayMinusOneMonth.getMonth().getValue());
        firstSubscription.setPaymentMethodExpirationYear((short) todayMinusOneMonth.getYear());

        subscriptionRepository.save(firstSubscription);
        SubscriptionDto expiredCardSubscriptionDto = subscriptionAdminController
                .getSubscription(firstSubscription.getSubscriptionId())
                .getBody();
        assert expiredCardSubscriptionDto != null;
        Assertions.assertTrue(subscriptionService.isExpiringCard(LocalDate.now(), expiredCardSubscriptionDto));

        List<SubscriptionDto> dtos = subscriptionAdminController.getSubscriptionsWithExpiringCard();

        SubscriptionDto filteredOne = dtos.stream().filter(subscriptionDto1 ->
                        Objects.equals(
                                subscriptionDto1.getSubscriptionId(),
                                firstSubscription.getSubscriptionId()))
                .collect(Collectors.toList()).get(0);

    }


}
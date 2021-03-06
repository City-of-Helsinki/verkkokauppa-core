package fi.hel.verkkokauppa.order.service.subscription;

import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionIdsDto;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.subscription.Period;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.order.test.utils.TestUtils;
import fi.hel.verkkokauppa.order.testing.annotations.RunIfProfile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Objects;
import java.util.Optional;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
class SubscriptionServiceTest extends TestUtils {
    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private OrderRepository orderRepository;

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
            subscriptionService.setSubscriptionEndDateFromOrder(foundOrder, foundSubscription);
            Assertions.assertEquals(foundSubscription.getEndDate(), foundOrder.getEndDate());
        }
    }

}
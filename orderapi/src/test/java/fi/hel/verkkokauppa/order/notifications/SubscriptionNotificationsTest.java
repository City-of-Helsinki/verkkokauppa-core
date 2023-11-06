package fi.hel.verkkokauppa.order.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.queue.service.SendNotificationService;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.data.DummyData;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderItemTransformer;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderTransformer;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderPaymentMethodRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.order.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.UUID;

import static java.lang.Thread.sleep;

@RunIfProfile(profile = "local")
@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@Slf4j
public class SubscriptionNotificationsTest extends DummyData {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

     @Autowired
    private SendNotificationService sendNotificationService;

    private ArrayList<String> toBeDeletedOrderById = new ArrayList<>();
    private ArrayList<String> toBeDeletedSubscriptionById = new ArrayList<>();


    @After
    public void tearDown() {
        try {
            toBeDeletedOrderById.forEach(orderId -> orderRepository.deleteById(orderId));
            toBeDeletedSubscriptionById.forEach(subscriptionId -> subscriptionRepository.deleteById(subscriptionId));

            // Clear list because all deleted
            toBeDeletedOrderById = new ArrayList<>();
            toBeDeletedSubscriptionById = new ArrayList<>();
        } catch (Exception e) {
            log.info("delete error {}", e.toString());
            toBeDeletedOrderById = new ArrayList<>();
            toBeDeletedSubscriptionById = new ArrayList<>();

        }
    }

    @Test
    @RunIfProfile(profile = "local")
    public void testSendSubscriptionCreatedNotification() throws Exception {

        SubscriptionMessage.SubscriptionMessageBuilder subscriptionMessageBuilder = SubscriptionMessage.builder()
                .subscriptionId(UUID.randomUUID().toString())
                .orderId(UUID.randomUUID().toString())
                .orderItemId(UUID.randomUUID().toString())
                .namespace("venepaikat")
                .eventType(EventType.SUBSCRIPTION_CREATED)
                .timestamp(DateTimeUtil.getDateTime())
                .eventTimestamp(DateTimeUtil.getDateTime());

        sendNotificationService.sendSubscriptionMessageNotification(subscriptionMessageBuilder.build());

        // TODO finalize test

    }

    @Test
    @RunIfProfile(profile = "local")
    public void testSendSubscriptionCardExpiredMessageNotification() throws Exception {
        Order order = generateDummyOrder();
        order.setOrderId(UUID.randomUUID().toString());
        order.setNamespace("venepaikat");
        order = orderRepository.save(order);

        //toBeDeletedOrderById.add(order.getOrderId());
        log.info("Order id: " +order.getOrderId());

        Subscription subscription = generateDummySubscription(order);
        subscription.setSubscriptionId(UUID.randomUUID().toString());
        subscription.setNamespace("venepaikat");
        subscriptionRepository.save(subscription);

        //toBeDeletedSubscriptionById.add(subscription.getSubscriptionId());
        log.info("Subscription id: " + subscription.getSubscriptionId());

        SubscriptionMessage.SubscriptionMessageBuilder subscriptionMessageBuilder = SubscriptionMessage.builder()
                .subscriptionId(subscription.getSubscriptionId())
                .orderId(order.getOrderId())
                .orderItemId(subscription.getOrderItemId())
                .namespace(subscription.getNamespace())
                .eventType(EventType.SUBSCRIPTION_CARD_EXPIRED)
                .timestamp(DateTimeUtil.getDateTime())
                .eventTimestamp(DateTimeUtil.getDateTime());

        sendNotificationService.sendSubscriptionMessageNotification(subscriptionMessageBuilder.build());

        // give queue some time to process
//        sleep(5000);

        // TODO finalize test and check email from mailhog (like in events api ErrorEmailNotificationListenerTest)

    }

}

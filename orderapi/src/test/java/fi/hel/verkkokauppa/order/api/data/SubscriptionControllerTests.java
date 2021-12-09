package fi.hel.verkkokauppa.order.api.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import fi.hel.verkkokauppa.order.api.OrderController;
import fi.hel.verkkokauppa.order.api.SubscriptionController;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionIdsDto;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderTransformerUtils;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.model.OrderItemMeta;
import fi.hel.verkkokauppa.order.model.subscription.Period;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionItemMetaRepository;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import fi.hel.verkkokauppa.order.service.subscription.CreateOrderFromSubscriptionCommand;
import fi.hel.verkkokauppa.order.service.subscription.GetSubscriptionQuery;
import fi.hel.verkkokauppa.order.service.subscription.CreateOrderFromSubscriptionCommand;
import fi.hel.verkkokauppa.order.service.subscription.GetSubscriptionQuery;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class SubscriptionControllerTests extends DummyData {

    @Autowired
    private SubscriptionItemMetaRepository subscriptionItemMetaRepository;

    @Autowired
    private OrderTransformerUtils orderTransformerUtils;

    @Autowired
    private OrderController orderController;

    @Autowired
    private SubscriptionController subscriptionController;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private CreateOrderFromSubscriptionCommand createOrderFromSubscriptionCommand;

    @Autowired
    private GetSubscriptionQuery getSubscriptionQuery;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;


    @Test
    public void assertTrue(){
        Assertions.assertTrue(true);
    }

    //This test is ignored because uses pure elastic search and not mocks to make testing easier.
    //@Test
    public void testCreateWithItems() throws JsonProcessingException {
        Order order = generateDummyOrder();
        orderRepository.delete(order);

        order.setNamespace("venepaikat");
        order.setCustomerEmail(UUID.randomUUID().toString() + "@ambientia.fi");
        List<OrderItem> orderItems = generateDummyOrderItemList(order, 2);
        orderItems.get(0).setPeriodFrequency(1L);
        orderItems.get(0).setPeriodUnit(Period.DAILY);
        orderItems.get(0).setPeriodCount(2);
        orderItems.get(0).setBillingStartDate(LocalDateTime.now());
        orderItems.get(0).setStartDate(LocalDateTime.now());
        orderItems.get(0).setPriceGross("124");
        List<OrderItemMeta> orderItemMetas = generateDummyOrderItemMetaList(orderItems);

        OrderAggregateDto orderAggregateDto = orderTransformerUtils
                .transformToOrderAggregateDto(order, orderItems, orderItemMetas);

        ResponseEntity<OrderAggregateDto> response = orderController.createWithItems(orderAggregateDto);

        ResponseEntity<SubscriptionIdsDto> createdSubs = subscriptionController.createSubscriptionsFromOrder(response.getBody());

        // Verify request succeed
        Assertions.assertEquals(HttpStatus.CREATED.value(), createdSubs.getStatusCodeValue());
        Assertions.assertEquals(1, Objects.requireNonNull(createdSubs.getBody().getSubscriptionIds()).size());

        // Read
        List<Subscription> subscriptions = subscriptionRepository.findByCustomerEmail(order.getCustomerEmail());
        Assertions.assertEquals(1L, (long) subscriptions.get(0).getPeriodFrequency());
        Assertions.assertEquals(Period.DAILY, subscriptions.get(0).getPeriodUnit());

        Assertions.assertEquals("active", subscriptions.get(0).getStatus());
        Assertions.assertEquals("venepaikat", subscriptions.get(0).getNamespace());
        Assertions.assertEquals("dummy_firstname", subscriptions.get(0).getCustomerFirstName());
        Assertions.assertEquals("dummy_lastname", subscriptions.get(0).getCustomerLastName());
        Assertions.assertEquals(order.getCustomerEmail(), subscriptions.get(0).getCustomerEmail());
        Assertions.assertEquals("dummy_user", subscriptions.get(0).getUser());
        Assertions.assertNotNull(subscriptions.get(0).getStartDate());
        Assertions.assertNotNull(subscriptions.get(0).getBillingStartDate());
        Assertions.assertEquals("daily", subscriptions.get(0).getPeriodUnit());

        Assertions.assertEquals(1L, (long) subscriptions.get(0).getPeriodFrequency());
        Assertions.assertEquals(2, (int) subscriptions.get(0).getPeriodCount());

        Assertions.assertEquals("productId", subscriptions.get(0).getProductId());
        Assertions.assertEquals("productName", subscriptions.get(0).getProductName());
        Assertions.assertEquals(1, (int) subscriptions.get(0).getQuantity());
        Assertions.assertEquals("124", subscriptions.get(0).getPriceGross());
        // New orderItemId should be created from order items.
        Assertions.assertNotEquals(orderItems.get(0).getOrderItemId(), subscriptions.get(0).getOrderItemId());
        // Order metas succesfully copied to subscription_item_metas
        Assertions.assertEquals(1, subscriptionItemMetaRepository.findByOrderItemId(subscriptions.get(0).getOrderItemId()).size());
        Assertions.assertEquals(subscriptions.get(0).getOrderId(), Objects.requireNonNull(response.getBody()).getOrder().getOrderId());


        // These checks if order can be purchased. KYV-233 and KYV-393
        SubscriptionDto subscriptionDto = getSubscriptionQuery.getOne(subscriptions.get(0).getSubscriptionId());
     }

    //This test is ignored because uses pure elastic search and not mocks to make testing easier.
//    @Test
    public void testCreateWithItemsGet() {
        Order order = generateDummyOrder();
        order.setNamespace("venepaikat");
        order.setCustomerEmail(UUID.randomUUID().toString() + "@ambientia.fi");
        List<OrderItem> orderItems = generateDummyOrderItemList(order, 2);
        orderItems.get(0).setPeriodFrequency(1L);
        orderItems.get(0).setPeriodUnit(Period.DAILY);
        orderItems.get(0).setPeriodCount(2);
        orderItems.get(0).setBillingStartDate(LocalDateTime.now());
        orderItems.get(0).setStartDate(LocalDateTime.now());
        orderItems.get(0).setPriceGross("124");
        List<OrderItemMeta> orderItemMetas = generateDummyOrderItemMetaList(orderItems);

        OrderAggregateDto orderAggregateDto = orderTransformerUtils
                .transformToOrderAggregateDto(order, orderItems, orderItemMetas);

        ResponseEntity<OrderAggregateDto> response = orderController.createWithItems(orderAggregateDto);

        OrderDto orderDto = Objects.requireNonNull(response.getBody()).getOrder();
        ResponseEntity<SubscriptionIdsDto> createdSubs = subscriptionController.createSubscriptionsFromOrderId(orderDto.getOrderId(),orderDto.getUser());

        // Verify request succeed
        Assertions.assertEquals(HttpStatus.CREATED.value(), createdSubs.getStatusCodeValue());
        Assertions.assertEquals(1, Objects.requireNonNull(createdSubs.getBody().getSubscriptionIds()).size());

        // Read
        List<Subscription> subscriptions = subscriptionRepository.findByCustomerEmail(order.getCustomerEmail());
        Assertions.assertEquals(1L, (long) subscriptions.get(0).getPeriodFrequency());
        Assertions.assertEquals(Period.DAILY, subscriptions.get(0).getPeriodUnit());

        Assertions.assertEquals("active", subscriptions.get(0).getStatus());
        Assertions.assertEquals("venepaikat", subscriptions.get(0).getNamespace());
        Assertions.assertEquals("dummy_firstname", subscriptions.get(0).getCustomerFirstName());
        Assertions.assertEquals("dummy_lastname", subscriptions.get(0).getCustomerLastName());
        Assertions.assertEquals(order.getCustomerEmail(), subscriptions.get(0).getCustomerEmail());
        Assertions.assertEquals("dummy_user", subscriptions.get(0).getUser());
        Assertions.assertNotNull(subscriptions.get(0).getStartDate());
        Assertions.assertNotNull(subscriptions.get(0).getBillingStartDate());
        Assertions.assertEquals("daily", subscriptions.get(0).getPeriodUnit());

        Assertions.assertEquals(1L, (long) subscriptions.get(0).getPeriodFrequency());
        Assertions.assertEquals(2, (int) subscriptions.get(0).getPeriodCount());

        Assertions.assertEquals("productId", subscriptions.get(0).getProductId());
        Assertions.assertEquals("productName", subscriptions.get(0).getProductName());
        Assertions.assertEquals(1, (int) subscriptions.get(0).getQuantity());
        Assertions.assertEquals("124", subscriptions.get(0).getPriceGross());
        // New orderItemId should be created from order items.
        Assertions.assertNotEquals(orderItems.get(0).getOrderItemId(), subscriptions.get(0).getOrderItemId());
        // Order metas succesfully copied to subscription_item_metas
        Assertions.assertEquals(1, subscriptionItemMetaRepository.findByOrderItemId(subscriptions.get(0).getOrderItemId()).size());
    }

}
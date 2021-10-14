package fi.hel.verkkokauppa.order.api.data;

import fi.hel.verkkokauppa.order.api.OrderController;
import fi.hel.verkkokauppa.order.api.SubscriptionController;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderTransformerUtils;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.model.OrderItemMeta;
import fi.hel.verkkokauppa.order.model.subscription.Period;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@RunWith(SpringJUnit4ClassRunner.class )
@SpringBootTest
public class OrderControllerTests extends DummyData {

//    @InjectMocks
//    private AccountingSlipService accountingSlipService;
//
//    @Mock
//    private OrderAccountingService mockOrderAccountingService;
//
//    @Mock
//    private OrderItemAccountingService mockOrderItemAccountingService;

    @Autowired
    private OrderTransformerUtils orderTransformerUtils;

    @Autowired
    private OrderController orderController;

    @Autowired
    private SubscriptionController subscriptionController;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    //This test is ignored because uses pure elastic search and not mocks to make testing easier.
    @Test
    public void testCreateWithItems() {
        Order order = generateDummyOrder();
        order.setNamespace("venepaikat");
        order.setCustomerEmail(UUID.randomUUID().toString() + "@ambientia.fi");
        List<OrderItem> orderItems = generateDummyOrderItemList(order,2);
        orderItems.get(0).setPeriodFrequency(1L);
        orderItems.get(0).setPeriodUnit(Period.DAILY);
        List<OrderItemMeta> orderItemMetas = generateDummyOrderItemMetaList(orderItems);

        OrderAggregateDto orderAggregateDto = orderTransformerUtils
                .transformToOrderAggregateDto(order,orderItems,orderItemMetas);

        ResponseEntity<OrderAggregateDto> response = orderController.createWithItems(orderAggregateDto);

        Objects.requireNonNull(response.getBody()).setNumberOfBillingCycles(1);

        ResponseEntity<Set<String>> createdSubs = subscriptionController.createSubscriptionsFromOrder(response.getBody());

        // Verify request succeed
        Assert.assertEquals(HttpStatus.CREATED.value(), createdSubs.getStatusCodeValue());
        Assert.assertEquals(1, Objects.requireNonNull(createdSubs.getBody()).size());

        // Read
        List<Subscription> subscriptions = subscriptionRepository.findByCustomerEmail(order.getCustomerEmail());
        Assert.assertEquals(1L, (long) subscriptions.get(0).getPeriodFrequency());
        Assert.assertEquals(Period.DAILY, subscriptions.get(0).getPeriodUnit());
    }

}
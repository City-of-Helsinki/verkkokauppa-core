package fi.hel.verkkokauppa.order.test.utils;

import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.OrderController;
import fi.hel.verkkokauppa.order.api.SubscriptionController;
import fi.hel.verkkokauppa.order.api.data.DummyData;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionIdsDto;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderTransformerUtils;
import fi.hel.verkkokauppa.order.logic.subscription.NextDateCalculator;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.model.OrderItemMeta;
import fi.hel.verkkokauppa.order.model.subscription.Period;
import fi.hel.verkkokauppa.order.model.subscription.Subscription;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
import fi.hel.verkkokauppa.order.repository.jpa.SubscriptionRepository;
import fi.hel.verkkokauppa.order.service.order.OrderItemService;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import fi.hel.verkkokauppa.order.service.subscription.SubscriptionService;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Component
public class TestUtils extends DummyData{

    @Autowired
    private OrderTransformerUtils orderTransformerUtils;

    @Autowired
    private OrderController orderController;

    @Autowired
    private SubscriptionController subscriptionController;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private NextDateCalculator nextDateCalculator;

    @Autowired
    private SubscriptionService subscriptionService;


    /**
     * Exclude field names by providing the field name as a string.
     * For example, if data object has a member variable "firstName" and you want to exclude it -
     * pass it inside a list as a string: Arrays.asList("firstName")
     * @param entity
     * @param exclusions
     * @return
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public static boolean hasNullFields(Object entity, List<String> exclusions) throws IllegalAccessException, IllegalArgumentException {
        for (Field f : entity.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            final String value = (String) f.get(entity);
            final String name = f.getName();
            boolean excludeNext = false;

            for (String exclusion : exclusions) {

                if (exclusion.equals(name)) {
                    excludeNext = true;
                    break;
                }
            }

            if (!excludeNext) {
                if (value == null) {
                    return true;
                }
            }
        }
        return false;
    }

    public ResponseEntity<OrderAggregateDto> generateSubscriptionOrderData(int itemCount, long periodFrequency, String periodUnit, int periodCount){
        Order order = generateDummyOrder();

        order.setNamespace("venepaikat");
        order.setCustomerEmail(UUID.randomUUID().toString() + "@ambientia.fi");
        List<OrderItem> orderItems = generateDummyOrderItemList(order, itemCount);
        orderItems.get(0).setPeriodFrequency(periodFrequency);
        orderItems.get(0).setPeriodUnit(periodUnit);
        orderItems.get(0).setPeriodCount(periodCount);
        orderItems.get(0).setBillingStartDate(LocalDateTime.now());
        orderItems.get(0).setStartDate(LocalDateTime.now());
        orderItems.get(0).setPriceGross("124");
        List<OrderItemMeta> orderItemMetas = generateDummyOrderItemMetaList(orderItems);

        OrderAggregateDto orderAggregateDto = orderTransformerUtils
                .transformToOrderAggregateDto(order, orderItems, orderItemMetas);

        return orderController.createWithItems(orderAggregateDto);
    }

    public ResponseEntity<OrderAggregateDto> createNewOrderToDatabase(int itemCount){
        Order order = generateDummyOrder();

        order.setNamespace("venepaikat");
        order.setCustomerEmail(UUID.randomUUID().toString() + "@ambientia.fi");
        List<OrderItem> orderItems = generateDummyOrderItemList(order, itemCount);
        orderItems.forEach(orderItem -> orderItem.setPriceGross("124"));
        List<OrderItemMeta> orderItemMetas = generateDummyOrderItemMetaList(orderItems);

        OrderAggregateDto orderAggregateDto = orderTransformerUtils
                .transformToOrderAggregateDto(order, orderItems, orderItemMetas);

        return orderController.createWithItems(orderAggregateDto);
    }


    public ResponseEntity<SubscriptionIdsDto> createSubscriptions(ResponseEntity<OrderAggregateDto> response){
        return subscriptionController.createSubscriptionsFromOrder(response.getBody());
    }

    public Subscription createAndGetMonthlySubscription(){
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

        String oneMonthFromTodayMinusOneDay = today
                .plus(1, ChronoUnit.MONTHS)
                .minus(1,ChronoUnit.DAYS)
                .format(formatter);

        Assertions.assertEquals(oneMonthFromTodayMinusOneDay, order1.getEndDate().format(formatter));
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
        // Asserts that endDate is moved + 1 month and -1 day from today date.
        Assertions.assertEquals(oneMonthFromTodayMinusOneDay, order1.getEndDate().format(formatter));

        // Is subscription created
        Assertions.assertNotNull(order1.getSubscriptionId());

        String firstSubscriptionId = order1.getSubscriptionId();
        // Fetch subscription and return
        return subscriptionService.findById(firstSubscriptionId);

    }
}

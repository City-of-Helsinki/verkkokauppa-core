package fi.hel.verkkokauppa.order.test.utils;

import fi.hel.verkkokauppa.order.api.OrderController;
import fi.hel.verkkokauppa.order.api.SubscriptionController;
import fi.hel.verkkokauppa.order.api.data.DummyData;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.subscription.SubscriptionIdsDto;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderTransformerUtils;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.model.OrderItemMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class TestUtils extends DummyData{

    @Autowired
    private OrderTransformerUtils orderTransformerUtils;

    @Autowired
    private OrderController orderController;

    @Autowired
    private SubscriptionController subscriptionController;

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

    public ResponseEntity<SubscriptionIdsDto> createSubscriptions(ResponseEntity<OrderAggregateDto> response){
        return subscriptionController.createSubscriptionsFromOrder(response.getBody());
    }
}

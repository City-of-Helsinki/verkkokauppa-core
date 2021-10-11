package fi.hel.verkkokauppa.order.api.data;

import fi.hel.verkkokauppa.order.api.OrderController;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderTransformerUtils;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.model.OrderItemMeta;
import fi.hel.verkkokauppa.order.model.subscription.Period;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

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

    //This test is ignored because uses pure elastic search and not mocks to make testing easier.
    @Test
    @Ignore
    public void testCreateWithItems() {
        Order order = generateDummyOrder();
        order.setNamespace("venepaikat");
        List<OrderItem> orderItems = generateDummyOrderItemList(order,2);
        orderItems.get(0).setPeriodFrequency(1L);
        orderItems.get(0).setPeriodUnit(Period.DAILY);
        List<OrderItemMeta> orderItemMetas = generateDummyOrderItemMetaList(orderItems);

        OrderAggregateDto orderAggregateDto = orderTransformerUtils
                .transformToOrderAggregateDto(order,orderItems,orderItemMetas);

        ResponseEntity<OrderAggregateDto> response = orderController.createWithItems(orderAggregateDto);

    }

}
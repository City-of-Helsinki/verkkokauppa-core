package fi.hel.verkkokauppa.order.service.order;

import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemMetaRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
class OrderItemServiceTest {
    @Mock
    private OrderItemMetaRepository orderItemMetaRepository;

    @Mock
    private OrderItemMetaService orderItemMetaService;
    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderItemService orderItemService;

    @Test
    void addItem() {
        String orderId = "1";
        doReturn("Success", "Success").when(orderItemRepository).save(new OrderItem());
        String orderItem1 = orderItemService.addItem(orderId, "productId", "productName", 1, "unit", "rowPriceNet", "rowPriceVat", "rowPriceTotal", "vatPercentage", "priceNet", "priceVat", "priceGross");
        String orderItem2 = orderItemService.addItem(orderId, "productId", "productName", 1, "unit", "rowPriceNet", "rowPriceVat", "rowPriceTotal", "vatPercentage", "priceNet", "priceVat", "priceGross");
        assertNotEquals(orderItem1,orderItem2);
    }
}
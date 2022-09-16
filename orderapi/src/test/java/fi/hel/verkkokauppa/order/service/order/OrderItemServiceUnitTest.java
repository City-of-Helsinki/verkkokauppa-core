package fi.hel.verkkokauppa.order.service.order;

import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemMetaRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemRepository;
import fi.hel.verkkokauppa.order.testing.utils.AutoMockBeanFactory;
import fi.hel.verkkokauppa.order.testing.annotations.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

@RunWith(SpringJUnit4ClassRunner.class)
@UnitTest
@WebMvcTest(OrderItemService.class)
@ContextConfiguration(classes = AutoMockBeanFactory.class) // This automatically mocks missing beans
class OrderItemServiceUnitTest {
    @Mock
    private OrderItemMetaRepository orderItemMetaRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderItemMetaService orderItemMetaService;

    @InjectMocks
    private OrderItemService orderItemService;

    @Test
    void addItem() {
        String orderId = "1";
        doReturn("Success", "Success").when(orderItemRepository).save(new OrderItem());
        String orderItem1 = orderItemService.addItem(
                orderId,
                "merchantId",
                "productId",
                "productName",
                "productLabel",
                "productDescription",
                1,
                "unit",
                "rowPriceNet",
                "rowPriceVat",
                "rowPriceTotal",
                "vatPercentage",
                "priceNet",
                "priceVat",
                "priceGross",
                "originalPriceNet",
                "originalPriceVat",
                "originalPriceGross",
                "",
                0L,
                0,
                null,
                null
        );
        String orderItem2 = orderItemService.addItem(
                orderId,
                "merchantId",
                "productId",
                "productName",
                "productLabel",
                "productDescription",
                1,
                "unit",
                "rowPriceNet",
                "rowPriceVat",
                "rowPriceTotal",
                "vatPercentage",
                "priceNet",
                "priceVat",
                "priceGross",
                "originalPriceNet",
                "originalPriceVat",
                "originalPriceGross",
                "",
                0L,
                0,
                null,
                null
        );
        assertNotEquals(orderItem1,orderItem2);
    }
}
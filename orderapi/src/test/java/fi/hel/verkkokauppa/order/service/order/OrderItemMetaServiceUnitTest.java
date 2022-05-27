package fi.hel.verkkokauppa.order.service.order;

import fi.hel.verkkokauppa.order.api.data.OrderItemMetaDto;
import fi.hel.verkkokauppa.order.model.OrderItemMeta;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemMetaRepository;
import fi.hel.verkkokauppa.order.testing.utils.AutoMockBeanFactory;
import fi.hel.verkkokauppa.order.testing.annotations.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.doReturn;

@RunWith(SpringJUnit4ClassRunner.class )
@UnitTest
@WebMvcTest(OrderItemMetaService.class)
@ContextConfiguration(classes = AutoMockBeanFactory.class) // This automatically mocks missing beans
class OrderItemMetaServiceUnitTest {
    @Mock
    private OrderItemMetaRepository orderItemMetaRepository;

    @InjectMocks
    private OrderItemMetaService orderItemMetaService;

    @Test
    void addItemMeta() {
        OrderItemMetaDto meta = new OrderItemMetaDto();
        meta.setOrderItemId("orderId");
        meta.setKey("keyTest");
        doReturn("Success", "Success").when(orderItemMetaRepository).save(new OrderItemMeta());
        String metaId1 = orderItemMetaService.addItemMeta(meta);
        String metaId2 = orderItemMetaService.addItemMeta(meta);
        assertNotEquals(metaId1,metaId2,"You should be able to add 2 ");
    }
}
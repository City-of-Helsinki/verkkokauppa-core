package fi.hel.verkkokauppa.order.service.order;

import fi.hel.verkkokauppa.order.api.data.OrderItemMetaDto;
import fi.hel.verkkokauppa.order.model.OrderItemMeta;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemMetaRepository;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class )
@SpringBootTest
class OrderItemMetaServiceTest {
    @Mock
    private OrderItemMetaRepository orderItemMetaRepository;

    @InjectMocks
    private OrderItemMetaService orderItemMetaService;

    @Test
    void addItemMeta() {
        OrderItemMetaDto meta = new OrderItemMetaDto();
        meta.setOrderItemId("orderId");
        meta.setKey("keyTest");
        String metaId1 = orderItemMetaService.addItemMeta(meta);
        String metaId2 = orderItemMetaService.addItemMeta(meta);
        assertNotEquals(metaId1,metaId2,"You should be able to add 2 ");
    }
}
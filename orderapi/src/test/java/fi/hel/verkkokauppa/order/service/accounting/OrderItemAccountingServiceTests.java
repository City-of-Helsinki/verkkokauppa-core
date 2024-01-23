package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.order.api.data.DummyData;
import fi.hel.verkkokauppa.order.api.data.accounting.CreateOrderAccountingRequestDto;
import fi.hel.verkkokauppa.order.api.data.accounting.OrderItemAccountingDto;
import fi.hel.verkkokauppa.order.api.data.accounting.ProductAccountingDto;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
import fi.hel.verkkokauppa.order.test.utils.AccountingTestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class OrderItemAccountingServiceTests extends AccountingTestUtils {

    @InjectMocks
    private AccountingSlipService accountingSlipService;

    @Mock
    private OrderAccountingService mockOrderAccountingService;

    @Mock
    private OrderItemAccountingService mockOrderItemAccountingService;

    @Mock
    private RefundItemAccountingService mockRefundItemAccountingService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderItemAccountingService orderItemAccountingService;

    @Test
    public void testOrderItemAccountingCreation() {
        // create dummy order for test
        Order order = createTestOrder();
        order.setOrderId(UUIDGenerator.generateType4UUID().toString());

        // create dummy orderItem with for test
        OrderItem orderItem = createTestOrderItem(order);

        // create order accounting request
        ProductAccountingDto dto = createDummyProductAccountingDto(orderItem.getProductId(), "1");
        ProductAccountingDto dto2 = createDummyProductAccountingDto("productId", "2");
        List<ProductAccountingDto> dtos = new ArrayList<>();
        dtos.add(dto2);
        dtos.add(dto);
        CreateOrderAccountingRequestDto request = new CreateOrderAccountingRequestDto();
        request.setDtos(dtos);
        request.setOrderId(order.getOrderId());

        List<OrderItemAccountingDto> orderItemAccountings = orderItemAccountingService.createOrderItemAccountings(request);

        assertEquals(orderItemAccountings.get(0).getPriceGross(), orderItem.getRowPriceTotal());
        assertEquals(orderItemAccountings.get(0).getPriceNet(), orderItem.getRowPriceNet());
        assertEquals(orderItemAccountings.get(0).getPriceVat(), orderItem.getRowPriceVat());

        assertEquals(orderItemAccountings.get(0).getInternalOrder(), dto.getInternalOrder());
        assertEquals(orderItemAccountings.get(0).getMainLedgerAccount(), dto.getMainLedgerAccount());
        assertEquals(orderItemAccountings.get(0).getCompanyCode(), dto.getCompanyCode());
        assertEquals(orderItemAccountings.get(0).getOperationArea(), dto.getOperationArea());
        assertEquals(orderItemAccountings.get(0).getProject(), dto.getProject());
        assertEquals(orderItemAccountings.get(0).getBalanceProfitCenter(), dto.getBalanceProfitCenter());
        assertEquals(orderItemAccountings.get(0).getVatCode(), dto.getVatCode());
        assertEquals(orderItemAccountings.get(0).getProfitCenter(), dto.getProfitCenter());

    }

}
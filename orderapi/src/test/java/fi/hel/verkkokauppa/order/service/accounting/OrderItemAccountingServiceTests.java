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
import fi.hel.verkkokauppa.order.service.order.OrderService;
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

    @Autowired
    private OrderService orderService;

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

    @Test
    public void testOrderItemAccountingCreationWitZeroRowsIncluded() {
        // create dummy order for test
        Order order = createTestOrder();
        order.setOrderId(UUIDGenerator.generateType4UUID().toString());

        // create dummy orderItems with for test
        OrderItem orderItem = createTestOrderItem(order);
        OrderItem freeOrderItem = createFreeTestOrderItem(order,"0.0");
        OrderItem secondFreeOrderItem = createFreeTestOrderItem(order,"0");

        // create order accounting request
        List<ProductAccountingDto> dtos = new ArrayList<>();
        dtos.add(createDummyProductAccountingDto(orderItem.getProductId(), "1"));
        dtos.add(createDummyProductAccountingDto(freeOrderItem.getProductId(), "2"));
        dtos.add(createDummyProductAccountingDto(secondFreeOrderItem.getProductId(), "3"));
        dtos.add(createDummyProductAccountingDto("productId", "4"));

        CreateOrderAccountingRequestDto request = new CreateOrderAccountingRequestDto();
        request.setDtos(dtos);
        request.setOrderId(order.getOrderId());

        List<OrderItemAccountingDto> orderItemAccountings = orderItemAccountingService.createOrderItemAccountings(request);

        assertEquals(orderItemAccountings.get(0).getPriceGross(), orderItem.getRowPriceTotal());
        assertEquals(orderItemAccountings.get(0).getPriceNet(), orderItem.getRowPriceNet());
        assertEquals(orderItemAccountings.get(0).getPriceVat(), orderItem.getRowPriceVat());

        assertEquals(orderItemAccountings.get(0).getInternalOrder(), dtos.get(0).getInternalOrder());
        assertEquals(orderItemAccountings.get(0).getMainLedgerAccount(), dtos.get(0).getMainLedgerAccount());
        assertEquals(orderItemAccountings.get(0).getCompanyCode(), dtos.get(0).getCompanyCode());
        assertEquals(orderItemAccountings.get(0).getOperationArea(), dtos.get(0).getOperationArea());
        assertEquals(orderItemAccountings.get(0).getProject(), dtos.get(0).getProject());
        assertEquals(orderItemAccountings.get(0).getBalanceProfitCenter(), dtos.get(0).getBalanceProfitCenter());
        assertEquals(orderItemAccountings.get(0).getVatCode(), dtos.get(0).getVatCode());
        assertEquals(orderItemAccountings.get(0).getProfitCenter(), dtos.get(0).getProfitCenter());

        assertEquals(orderItemAccountings.size(), 1);
    }

}
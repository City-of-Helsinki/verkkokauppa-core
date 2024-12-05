package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.order.api.data.accounting.*;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.model.refund.Refund;
import fi.hel.verkkokauppa.order.model.refund.RefundItem;
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
public class RefundItemAccountingServiceTests extends AccountingTestUtils {

    @InjectMocks
    private AccountingSlipService accountingSlipService;

    @Mock
    private OrderAccountingService mockOrderAccountingService;

    @Mock
    private OrderItemAccountingService mockOrderItemAccountingService;

    @Mock
    private RefundItemAccountingService mockRefundItemAccountingService;

    @Autowired
    private RefundItemAccountingService refundItemAccountingService;

    @Test
    public void testRefundItemAccountingCreation() {
        // create dummy order for test
        Order order = createTestOrder();
        order.setOrderId(UUIDGenerator.generateType4UUID().toString());
        Refund refund = createTestRefund(order.getOrderId());

        // create dummy orderItem with for test
        RefundItem refundItem = createTestRefundItem(refund);

        // create order accounting request
        ProductAccountingDto dto = createDummyProductAccountingDto(refundItem.getProductId(), "1");
        ProductAccountingDto dto2 = createDummyProductAccountingDto("productId", "2");
        List<ProductAccountingDto> dtos = new ArrayList<>();
        dtos.add(dto2);
        dtos.add(dto);
        CreateRefundAccountingRequestDto request = new CreateRefundAccountingRequestDto();
        request.setDtos(dtos);
        request.setOrderId(order.getOrderId());
        request.setRefundId(refund.getRefundId());


        List<RefundItemAccountingDto> refundItemAccountings = refundItemAccountingService.createRefundItemAccountings(request);

        assertEquals(refundItemAccountings.get(0).getPriceGross(), refundItem.getRowPriceTotal());
        assertEquals(refundItemAccountings.get(0).getPriceNet(), refundItem.getRowPriceNet());
        assertEquals(refundItemAccountings.get(0).getPriceVat(), refundItem.getRowPriceVat());

        assertEquals(refundItemAccountings.get(0).getInternalOrder(), dto.getInternalOrder());
        assertEquals(refundItemAccountings.get(0).getMainLedgerAccount(), dto.getMainLedgerAccount());
        assertEquals(refundItemAccountings.get(0).getCompanyCode(), dto.getCompanyCode());
        assertEquals(refundItemAccountings.get(0).getOperationArea(), dto.getOperationArea());
        assertEquals(refundItemAccountings.get(0).getProject(), dto.getProject());
        assertEquals(refundItemAccountings.get(0).getBalanceProfitCenter(), dto.getBalanceProfitCenter());
        assertEquals(refundItemAccountings.get(0).getVatCode(), dto.getVatCode());
        assertEquals(refundItemAccountings.get(0).getProfitCenter(), dto.getProfitCenter());

    }

    @Test
    public void testRefundItemAccountingCreationWitZeroPriceItems() {
        // create dummy order for test
        Order order = createTestOrder();
        order.setOrderId(UUIDGenerator.generateType4UUID().toString());
        Refund refund = createTestRefund(order.getOrderId());

        // create dummy orderItem with for test
        RefundItem refundItem = createTestRefundItem(refund);
        RefundItem refundItemFree = createTestRefundItemWithPrice(refund,"0","0","0");
        RefundItem refundItemFree2 = createTestRefundItemWithPrice(refund,"0.0","0.0","0.0");

        // create order accounting request
        ProductAccountingDto dto = createDummyProductAccountingDto(refundItem.getProductId(), "1");
        ProductAccountingDto dto2 = createDummyProductAccountingDto("productId", "2");
        ProductAccountingDto dto3 = createDummyProductAccountingDto("productId", "2");
        List<ProductAccountingDto> dtos = new ArrayList<>();
        dtos.add(dto3);
        dtos.add(dto2);
        dtos.add(dto);
        CreateRefundAccountingRequestDto request = new CreateRefundAccountingRequestDto();
        request.setDtos(dtos);
        request.setOrderId(order.getOrderId());
        request.setRefundId(refund.getRefundId());


        List<RefundItemAccountingDto> refundItemAccountings = refundItemAccountingService.createRefundItemAccountings(request);

        assertEquals(refundItemAccountings.get(0).getPriceGross(), refundItem.getRowPriceTotal());
        assertEquals(refundItemAccountings.get(0).getPriceNet(), refundItem.getRowPriceNet());
        assertEquals(refundItemAccountings.get(0).getPriceVat(), refundItem.getRowPriceVat());

        assertEquals(refundItemAccountings.get(0).getInternalOrder(), dto.getInternalOrder());
        assertEquals(refundItemAccountings.get(0).getMainLedgerAccount(), dto.getMainLedgerAccount());
        assertEquals(refundItemAccountings.get(0).getCompanyCode(), dto.getCompanyCode());
        assertEquals(refundItemAccountings.get(0).getOperationArea(), dto.getOperationArea());
        assertEquals(refundItemAccountings.get(0).getProject(), dto.getProject());
        assertEquals(refundItemAccountings.get(0).getBalanceProfitCenter(), dto.getBalanceProfitCenter());
        assertEquals(refundItemAccountings.get(0).getVatCode(), dto.getVatCode());
        assertEquals(refundItemAccountings.get(0).getProfitCenter(), dto.getProfitCenter());

        assertEquals(refundItemAccountings.size(), 1);
    }

}
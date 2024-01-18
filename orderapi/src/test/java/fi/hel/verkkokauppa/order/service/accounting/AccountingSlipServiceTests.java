package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.data.DummyData;
import fi.hel.verkkokauppa.order.api.data.accounting.OrderItemAccountingDto;
import fi.hel.verkkokauppa.order.api.data.accounting.RefundItemAccountingDto;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderItemAccountingTransformer;
import fi.hel.verkkokauppa.order.api.data.transformer.RefundItemAccountingTransformer;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.accounting.OrderAccounting;
import fi.hel.verkkokauppa.order.model.accounting.OrderItemAccounting;
import fi.hel.verkkokauppa.order.model.accounting.RefundItemAccounting;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class )
@SpringBootTest
public class AccountingSlipServiceTests extends DummyData {

    @InjectMocks
    private AccountingSlipService accountingSlipService;

    @Mock
    private OrderAccountingService mockOrderAccountingService;

    @Mock
    private OrderItemAccountingService mockOrderItemAccountingService;

    @Mock
    private RefundItemAccountingService mockRefundItemAccountingService;

    @Test
    public void testGroupingByDateGroupsAccountingIdsCorrectly() {
        ArrayList<Order> orders = generateDummyOrderList();

        List<String> orderIds = orders.stream()
                .map(Order::getOrderId)
                .collect(Collectors.toList());

        List<OrderAccounting> orderAccountings = generateDummyOrderAccountingList();

        Map<LocalDate, List<String>> result = accountingSlipService.groupOrderAccountingsByDate(orderAccountings);

        assertFalse(result.isEmpty());

        Map<LocalDate, List<String>> expectedResult = new HashMap<>();
        expectedResult.put(DateTimeUtil.fromFormattedDateString("2021-09-01").toLocalDate(), List.of("1", "2"));
        expectedResult.put(DateTimeUtil.fromFormattedDateString("2021-09-02").toLocalDate(), Collections.singletonList("3"));
        assertEquals(expectedResult, result);
    }

    @Test
    public void testORderAccountingsAreSummedPerPostingForEachCompanyCode() {
        Map<LocalDate, List<String>> accountingsForDate = new HashMap<>();
        accountingsForDate.put(DateTimeUtil.fromFormattedDateString("2021-09-01").toLocalDate(), List.of("1", "2"));
        Set<Map.Entry<LocalDate, List<String>>> entries = accountingsForDate.entrySet();

        String companyCode1 = "1234";
        OrderItemAccounting orderItemAccounting1 = new OrderItemAccounting("1", "1", "20", "10", "10", companyCode1, "account", "24", "yes",
                "profitCenter", "balanceProfitCenter", "project 1", "Area A");
        OrderItemAccounting orderItemAccounting2 = new OrderItemAccounting("2", "2", "10", "5", "5", companyCode1, "account", "24", "yes",
                "profitCenter", "balanceProfitCenter", "project 2", "Area B");
        OrderItemAccounting orderItemAccounting3 = new OrderItemAccounting("3", "2", "10", "5", "5", companyCode1, "account", "24", "yes",
                "profitCenter", "balanceProfitCenter", "project 2", "Area B");

        String companyCode2 = "5678";
        OrderItemAccounting orderItemAccounting4 = new OrderItemAccounting("2", "2", "10", "5", "5", companyCode2, "account", "24", "yes",
                "profitCenter", "balanceProfitCenter", "project 2", "Area B");
        OrderItemAccounting orderItemAccounting5 = new OrderItemAccounting("3", "2", "10", "5", "5", companyCode2, "account", "24", "yes",
                "profitCenter", "balanceProfitCenter", "project 2", "Area B");

        List<OrderItemAccounting> list = new ArrayList<>();
        list.add(orderItemAccounting2);
        list.add(orderItemAccounting3);
        list.add(orderItemAccounting4);
        list.add(orderItemAccounting5);

        when(mockOrderItemAccountingService.getOrderItemAccountings("1")).thenReturn(Collections.singletonList(orderItemAccounting1));
        when(mockOrderItemAccountingService.getOrderItemAccountings("2")).thenReturn(list);

        Map<String, List<OrderItemAccountingDto>> summedOrderItemAccountingsForDate = null;
        for (Map.Entry<LocalDate, List<String>> entry : entries) {
            summedOrderItemAccountingsForDate = accountingSlipService.getSummedOrderItemAccountingsForDate(entry.getValue());
        }

        List<OrderItemAccountingDto> dtosForCompanyCode1 = summedOrderItemAccountingsForDate.get(companyCode1);
        List<OrderItemAccountingDto> resultListForCompanyCode1 = new ArrayList<>(dtosForCompanyCode1);

        List<OrderItemAccountingDto> expectedListForCompanyCode1 = new ArrayList<>();
        expectedListForCompanyCode1.add(new OrderItemAccountingDto("2", "2", "20", "10", "10", companyCode1, "account", "24", "yes", "profitCenter", "balanceProfitCenter", "project 2", "Area B"));
        expectedListForCompanyCode1.add(new OrderItemAccountingTransformer().transformToDto(orderItemAccounting1));

        assertEquals(expectedListForCompanyCode1, resultListForCompanyCode1);

        List<OrderItemAccountingDto> dtosForCompanyCode2 = summedOrderItemAccountingsForDate.get(companyCode2);
        List<OrderItemAccountingDto> resultListForCompanyCode2 = new ArrayList<>(dtosForCompanyCode2);

        List<OrderItemAccountingDto> expectedListForCompanyCode2 = new ArrayList<>();
        expectedListForCompanyCode2.add(new OrderItemAccountingDto("2", "2", "20", "10", "10", companyCode2, "account", "24", "yes", "profitCenter", "balanceProfitCenter", "project 2", "Area B"));

        assertEquals(expectedListForCompanyCode2, resultListForCompanyCode2);
    }

    @Test
    public void testRefundAccountingsAreSummedPerPostingForEachCompanyCode() {
        Map<LocalDate, List<String>> accountingsForDate = new HashMap<>();
        accountingsForDate.put(DateTimeUtil.fromFormattedDateString("2021-09-01").toLocalDate(), List.of("1", "2"));
        Set<Map.Entry<LocalDate, List<String>>> entries = accountingsForDate.entrySet();

        String companyCode1 = "1234";
        RefundItemAccounting refundItemAccounting1 = new RefundItemAccounting("1", "1", "1", "20", "10", "10", companyCode1, "account", "24", "yes",
                "profitCenter", "balanceProfitCenter", "project 1", "Area A");
        RefundItemAccounting refundItemAccounting2 = new RefundItemAccounting("2", "2", "2", "10", "5", "5", companyCode1, "account", "24", "yes",
                "profitCenter", "balanceProfitCenter", "project 2", "Area B");
        RefundItemAccounting refundItemAccounting3 = new RefundItemAccounting("3", "2", "2", "10", "5", "5", companyCode1, "account", "24", "yes",
                "profitCenter", "balanceProfitCenter", "project 2", "Area B");

        String companyCode2 = "5678";
        RefundItemAccounting refundItemAccounting4 = new RefundItemAccounting("4", "2", "2", "10", "5", "5", companyCode2, "account", "24", "yes",
                "profitCenter", "balanceProfitCenter", "project 2", "Area B");
        RefundItemAccounting refundItemAccounting5 = new RefundItemAccounting("5", "2", "2", "10", "5", "5", companyCode2, "account", "24", "yes",
                "profitCenter", "balanceProfitCenter", "project 2", "Area B");

        List<RefundItemAccounting> list = new ArrayList<>();
        list.add(refundItemAccounting2);
        list.add(refundItemAccounting3);
        list.add(refundItemAccounting4);
        list.add(refundItemAccounting5);

        when(mockRefundItemAccountingService.getRefundItemAccountings("1")).thenReturn(Collections.singletonList(refundItemAccounting1));
        when(mockRefundItemAccountingService.getRefundItemAccountings("2")).thenReturn(list);

        Map<String, List<RefundItemAccountingDto>> summedRefundItemAccountingsForDate = null;
        for (Map.Entry<LocalDate, List<String>> entry : entries) {
            summedRefundItemAccountingsForDate = accountingSlipService.getSummedRefundItemAccountingsForDate(entry.getValue());
        }

        List<RefundItemAccountingDto> dtosForCompanyCode1 = summedRefundItemAccountingsForDate.get(companyCode1);
        List<RefundItemAccountingDto> resultListForCompanyCode1 = new ArrayList<>(dtosForCompanyCode1);

        List<RefundItemAccountingDto> expectedListForCompanyCode1 = new ArrayList<>();
        expectedListForCompanyCode1.add(new RefundItemAccountingDto("2", "2", "2", "20", "10", "10", companyCode1, "account", "24", "yes", "profitCenter", "balanceProfitCenter", "project 2", "Area B"));
        expectedListForCompanyCode1.add(new RefundItemAccountingTransformer().transformToDto(refundItemAccounting1));

        assertEquals(expectedListForCompanyCode1, resultListForCompanyCode1);

        List<RefundItemAccountingDto> dtosForCompanyCode2 = summedRefundItemAccountingsForDate.get(companyCode2);
        List<RefundItemAccountingDto> resultListForCompanyCode2 = new ArrayList<>(dtosForCompanyCode2);

        List<RefundItemAccountingDto> expectedListForCompanyCode2 = new ArrayList<>();
        expectedListForCompanyCode2.add(new RefundItemAccountingDto("2", "2", "2", "20", "10", "10", companyCode2, "account", "24", "yes", "profitCenter", "balanceProfitCenter", "project 2", "Area B"));

        assertEquals(expectedListForCompanyCode2, resultListForCompanyCode2);
    }


}

package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.data.DummyData;
import fi.hel.verkkokauppa.order.api.data.accounting.OrderItemAccountingDto;
import fi.hel.verkkokauppa.order.api.data.transformer.OrderItemAccountingTransformer;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.accounting.OrderAccounting;
import fi.hel.verkkokauppa.order.model.accounting.OrderItemAccounting;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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

    @Test
    public void testGroupingByDateGroupsAccountingIdsCorrectly() {
        ArrayList<Order> orders = generateDummyOrderList();

        List<String> orderIds = orders.stream()
                .map(Order::getOrderId)
                .collect(Collectors.toList());

        List<OrderAccounting> orderAccountings = generateDummyOrderAccountingList();

        when(mockOrderAccountingService.getOrderAccountings(orderIds)).thenReturn(orderAccountings);
        Map<LocalDateTime, List<String>> result = accountingSlipService.groupAccountingsByDate(orders);

        assertFalse(result.isEmpty());

        Map<LocalDateTime, List<String>> expectedResult = new HashMap<>();
        expectedResult.put(DateTimeUtil.fromFormattedDateString("2021-09-01"), List.of("1", "2"));
        expectedResult.put(DateTimeUtil.fromFormattedDateString("2021-09-02"), Collections.singletonList("3"));
        assertEquals(expectedResult, result);
    }

    @Test
    public void testAccountingsAreSummedPerPostingForEachCompanyCode() {
        Map<LocalDateTime, List<String>> accountingsForDate = new HashMap<>();
        accountingsForDate.put(DateTimeUtil.fromFormattedDateString("2021-09-01"), List.of("1", "2"));
        Set<Map.Entry<LocalDateTime, List<String>>> entries = accountingsForDate.entrySet();

        String companyCode1 = "1234";
        OrderItemAccounting orderItemAccounting1 = new OrderItemAccounting("1", "1", "20", "10", "10", companyCode1, "account", "24", "yes",
                "profitCenter", "project 1", "Area A");
        OrderItemAccounting orderItemAccounting2 = new OrderItemAccounting("2", "2", "10", "5", "5", companyCode1, "account", "24", "yes",
                "profitCenter", "project 2", "Area B");
        OrderItemAccounting orderItemAccounting3 = new OrderItemAccounting("3", "2", "10", "5", "5", companyCode1, "account", "24", "yes",
                "profitCenter", "project 2", "Area B");

        String companyCode2 = "5678";
        OrderItemAccounting orderItemAccounting4 = new OrderItemAccounting("2", "2", "10", "5", "5", companyCode2, "account", "24", "yes",
                "profitCenter", "project 2", "Area B");
        OrderItemAccounting orderItemAccounting5 = new OrderItemAccounting("3", "2", "10", "5", "5", companyCode2, "account", "24", "yes",
                "profitCenter", "project 2", "Area B");

        List<OrderItemAccounting> list = new ArrayList<>();
        list.add(orderItemAccounting2);
        list.add(orderItemAccounting3);
        list.add(orderItemAccounting4);
        list.add(orderItemAccounting5);

        when(mockOrderItemAccountingService.getOrderItemAccountings("1")).thenReturn(Collections.singletonList(orderItemAccounting1));
        when(mockOrderItemAccountingService.getOrderItemAccountings("2")).thenReturn(list);

        Map<String, List<OrderItemAccountingDto>> summedOrderItemAccountingsForDate = null;
        for (Map.Entry<LocalDateTime, List<String>> entry : entries) {
            summedOrderItemAccountingsForDate = accountingSlipService.getSummedOrderItemAccountingsForDate(entry);
        }

        List<OrderItemAccountingDto> dtosForCompanyCode1 = summedOrderItemAccountingsForDate.get(companyCode1);
        List<OrderItemAccountingDto> resultListForCompanyCode1 = new ArrayList<>(dtosForCompanyCode1);

        List<OrderItemAccountingDto> expectedListForCompanyCode1 = new ArrayList<>();
        expectedListForCompanyCode1.add(new OrderItemAccountingDto("2", "2", "20", "10", "10", companyCode1, "account", "24", "yes", "profitCenter", "project 2", "Area B"));
        expectedListForCompanyCode1.add(new OrderItemAccountingTransformer().transformToDto(orderItemAccounting1));

        assertEquals(expectedListForCompanyCode1, resultListForCompanyCode1);

        List<OrderItemAccountingDto> dtosForCompanyCode2 = summedOrderItemAccountingsForDate.get(companyCode2);
        List<OrderItemAccountingDto> resultListForCompanyCode2 = new ArrayList<>(dtosForCompanyCode2);

        List<OrderItemAccountingDto> expectedListForCompanyCode2 = new ArrayList<>();
        expectedListForCompanyCode2.add(new OrderItemAccountingDto("2", "2", "20", "10", "10", companyCode2, "account", "24", "yes", "profitCenter", "project 2", "Area B"));

        assertEquals(expectedListForCompanyCode2, resultListForCompanyCode2);
    }

}
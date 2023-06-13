package fi.hel.verkkokauppa.order.api;

import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.data.DummyData;
import fi.hel.verkkokauppa.order.constants.RefundAccountingStatusEnum;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.accounting.*;
import fi.hel.verkkokauppa.order.model.refund.Refund;
import fi.hel.verkkokauppa.order.constants.RefundAccountingStatusEnum;
import fi.hel.verkkokauppa.order.repository.jpa.*;
import fi.hel.verkkokauppa.order.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// start local sftp server:
// verkkokauppa-core/docker compose up sftp
@RunIfProfile(profile = "local")
@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@Slf4j
public class AccountingControllerTest extends DummyData {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private OrderAccountingRepository orderAccountingRepository;

    @Autowired
    private RefundAccountingRepository refundAccountingRepository;

    @Autowired
    private OrderItemAccountingRepository orderItemAccountingRepository;

    @Autowired
    private RefundItemAccountingRepository refundItemAccountingRepository;

    private ArrayList<String> toBeDeletedOrderById = new ArrayList<>();
    private ArrayList<String> toBeDeletedOrderAccountingById = new ArrayList<>();
    private ArrayList<String> toBeDeletedOrderItemAccountingById = new ArrayList<>();
    private ArrayList<String> toBeDeletedRefundById = new ArrayList<>();
    private ArrayList<String> toBeDeletedRefundAccountingById = new ArrayList<>();
    private ArrayList<String> toBeDeletedRefundItemAccountingById = new ArrayList<>();

    @After
    public void tearDown() {
        try {
            toBeDeletedOrderById.forEach(orderId -> orderRepository.deleteById(orderId));
            toBeDeletedRefundById.forEach(refundId -> refundRepository.deleteById(refundId));
            toBeDeletedOrderAccountingById.forEach(id -> orderAccountingRepository.deleteById(id));
            toBeDeletedRefundAccountingById.forEach(id -> refundAccountingRepository.deleteById(id));
            toBeDeletedOrderItemAccountingById.forEach(id -> orderItemAccountingRepository.deleteById(id));
            toBeDeletedRefundItemAccountingById.forEach(id -> refundItemAccountingRepository.deleteById(id));
            toBeDeletedOrderById = new ArrayList<>();
            toBeDeletedOrderAccountingById = new ArrayList<>();
            toBeDeletedOrderItemAccountingById = new ArrayList<>();
            toBeDeletedRefundById = new ArrayList<>();
            toBeDeletedRefundAccountingById = new ArrayList<>();
            toBeDeletedRefundItemAccountingById = new ArrayList<>();
        } catch (Exception e) {
            log.info("delete error {}", e.toString());
            toBeDeletedOrderById = new ArrayList<>();
            toBeDeletedOrderAccountingById = new ArrayList<>();
            toBeDeletedOrderItemAccountingById = new ArrayList<>();
            toBeDeletedRefundById = new ArrayList<>();
            toBeDeletedRefundAccountingById = new ArrayList<>();
            toBeDeletedRefundItemAccountingById = new ArrayList<>();
        }
    }

    @Test
    @RunIfProfile(profile = "local")
    public void testAccountingCreate() throws Exception {
        Order order1 = createTestOrder();
        Order order2 = createTestOrder();
        Order order3 = createTestOrder();

        createTestOrderAccounting(order1.getOrderId());
        createTestOrderAccounting(order2.getOrderId());

        Refund refund1 = createTestRefund(order1.getOrderId());
        setTestRefundAccountingStatus(refund1.getRefundId(), RefundAccountingStatusEnum.CREATED);

        Refund refund2 = createTestRefund(order2.getOrderId());

        createTestRefundAccounting(refund1.getRefundId(), refund1.getOrderId());

        String companyCode1 = "1234";
        createTestOrderItemAccounting(
                order1.getOrderId(),
                "10", "7","3",
                companyCode1,
                "account",
                "24",
                "yes",
                "profitCenter",
                "balanceProfitCenter",
                "project 1",
                "Area A"
        );
        createTestOrderItemAccounting(
                order1.getOrderId(),
                "20", "10", "10",
                companyCode1,
                "account",
                "24",
                "yes",
                "profitCenter",
                "balanceProfitCenter2",
                "project 2",
                "Area B"
        );
        createTestOrderItemAccounting(
                order1.getOrderId(),
                "30", "20", "10",
                companyCode1,
                "account",
                "24",
                "yes",
                "profitCenter",
                "balanceProfitCenter2",
                "project 2",
                "Area B"
        );
        createTestOrderItemAccounting(
                order1.getOrderId(),
                "50", "35", "15",
                companyCode1,
                "account",
                "24",
                "yes",
                "profitCenter",
                "balanceProfitCenter2",
                "project 3",
                "Area A"
        );
        // add refunds
        createTestRefundItemAccounting(
                refund1.getRefundId(),
                refund1.getOrderId(),
                "-10", "-7", "-3",
                companyCode1,
                "account",
                "24",
                "yes",
                "profitCenter",
                "balanceProfitCenter",
                "project 1",
                "Area A"
        );
        createTestRefundItemAccounting(
                refund1.getRefundId(),
                refund1.getOrderId(),
                "-15", "-10", "-5",
                companyCode1,
                "account",
                "24",
                "yes",
                "profitCenter",
                "balanceProfitCenter2",
                "project 3",
                "Area A"
        );
        createTestRefundItemAccounting(
                refund1.getRefundId(),
                refund1.getOrderId(),
                "-25", "-15", "-10",
                companyCode1,
                "account",
                "24",
                "yes",
                "profitCenter",
                "balanceProfitCenter2",
                "project 3",
                "Area A"
        );
        //
        // second company code
        //
        String companyCode2 = "5678";
        createTestOrderItemAccounting(
                order2.getOrderId(),
                "10", "5", "5",
                companyCode2,
                "account",
                "24",
                "yes",
                "profitCenter",
                "balanceProfitCenter3",
                "project 2",
                "Area B"
        );
        createTestOrderItemAccounting(
                order2.getOrderId(),
                "10", "5", "5",
                companyCode2,
                "account",
                "24",
                "yes",
                "profitCenter",
                "balanceProfitCenter3",
                "project 2",
                "Area B"
        );

        this.mockMvc.perform(
                get("/accounting/create")
            )
            .andDo(print())
            .andExpect(status().is2xxSuccessful())
            .andExpect(status().is(200))
            .andReturn();

        order1 = orderRepository.findById(order1.getOrderId()).orElseThrow();
        assertNotNull(order1.getAccounted(), "Order 1 should be accounted");
        order2 = orderRepository.findById(order2.getOrderId()).orElseThrow();
        assertNotNull(order2.getAccounted(), "Order 2 should be accounted");
        refund1 = refundRepository.findById(refund1.getRefundId()).orElseThrow();
        assertNotNull(refund1.getAccounted(), "Refund 1 should be accounted");
        assertEquals(refund1.getAccountingStatus(), RefundAccountingStatusEnum.EXPORTED, "Refund1 accounting status should be exported");
        refund2 = refundRepository.findById(refund2.getRefundId()).orElseThrow();
        assertNull(refund2.getAccounted(), "Refund 2 should not be accounted");
        assertNull(refund2.getAccountingStatus(), "Refund 2 should not have accounting status set");
        order3 = orderRepository.findById(order3.getOrderId()).orElseThrow();
        assertNull(order3.getAccounted(), "Order 3 should not be accounted");

        // add another refund accounting, refund created earlier
        setTestRefundAccountingStatus(refund2.getRefundId(), RefundAccountingStatusEnum.CREATED);
        createTestRefundAccounting(refund2.getRefundId(), refund2.getOrderId());
        createTestRefundItemAccounting(
                refund2.getRefundId(),
                refund2.getOrderId(),
                "-10", "-5", "-5",
                companyCode2,
                "account",
                "24",
                "yes",
                "profitCenter",
                "balanceProfitCenter3",
                "project 2",
                "Area B"
        );

        // add another order accounting, order created earlier
        createTestOrderAccounting(order3.getOrderId());
        createTestOrderItemAccounting(
                refund2.getOrderId(),
                "10", "5", "5",
                companyCode1,
                "account",
                "24",
                "yes",
                "profitCenter",
                "balanceProfitCenter",
                "project 2",
                "Area B"
        );

        this.mockMvc.perform(
                        get("/accounting/create")
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(status().is(200))
                .andReturn();

        refund2 = refundRepository.findById(refund2.getRefundId()).orElseThrow();
        assertNotNull(refund2.getAccounted(), "Refund 2 should be accounted");
        assertEquals(refund2.getAccountingStatus(), RefundAccountingStatusEnum.EXPORTED, "Refund2 accounting status should be exported");
        order3 = orderRepository.findById(order3.getOrderId()).orElseThrow();
        assertNotNull(order3.getAccounted(), "Order 3 should be accounted");
    }


    private Order createTestOrder(){
        Order order = generateDummyOrder();
        order.setOrderId(UUID.randomUUID().toString());
        order = orderRepository.save(order);
        toBeDeletedOrderById.add(order.getOrderId());
        return order;
    }

    private OrderAccounting createTestOrderAccounting(String orderId) {
        OrderAccounting orderAccounting = new OrderAccounting();
        orderAccounting.setOrderId(orderId);
        orderAccounting.setCreatedAt(DateTimeUtil.getFormattedDateTime().minusDays(1));
        orderAccounting = orderAccountingRepository.save(orderAccounting);
        toBeDeletedOrderAccountingById.add(orderAccounting.getOrderId());

        return orderAccounting;
    }

    private OrderItemAccounting createTestOrderItemAccounting(String orderId, String priceGross, String priceNet, String priceVat,
                                                              String companyCode, String mainLedgerAccount, String vatCode,
                                                              String internalOrder, String profitCenter, String balanceProfitCenter,
                                                              String project, String operationArea){
        OrderItemAccounting orderItemAccounting = new OrderItemAccounting(
                UUID.randomUUID().toString(),
                orderId,
                priceGross,
                priceNet,
                priceVat,
                companyCode,
                mainLedgerAccount,
                vatCode,
                internalOrder,
                profitCenter,
                balanceProfitCenter,
                project,
                operationArea);

        orderItemAccounting = orderItemAccountingRepository.save(orderItemAccounting);
        toBeDeletedOrderItemAccountingById.add(orderItemAccounting.getOrderItemId());

        return orderItemAccounting;
    }

    private Refund createTestRefund(String orderId){
        Refund refund = generateDummyRefund(orderId);
        refund.setRefundId(UUID.randomUUID().toString());
        refund = refundRepository.save(refund);
        toBeDeletedRefundById.add(refund.getRefundId());
        return refund;
    }

    private Refund setTestRefundAccountingStatus(String refundId, RefundAccountingStatusEnum accountingStatus){
        Optional<Refund> returnedRefund = refundRepository.findById(refundId);
        Refund refund = returnedRefund.get();
        if( refund != null ) {
            refund.setAccountingStatus(accountingStatus);
            refund = refundRepository.save(refund);
        }
        return refund;
    }

    private RefundAccounting createTestRefundAccounting(String refundId, String orderId) {
        RefundAccounting refundAccounting = new RefundAccounting();
        refundAccounting.setRefundId(refundId);
        refundAccounting.setOrderId(orderId);
        refundAccounting.setCreatedAt(DateTimeUtil.getFormattedDateTime().minusDays(1));
        refundAccounting = refundAccountingRepository.save(refundAccounting);
        toBeDeletedRefundAccountingById.add(refundAccounting.getRefundId());

        return refundAccounting;
    }

    private RefundItemAccounting createTestRefundItemAccounting(String refundId, String orderId, String priceGross, String priceNet, String priceVat,
                                                              String companyCode, String mainLedgerAccount, String vatCode,
                                                              String internalOrder, String profitCenter, String balanceProfitCenter,
                                                              String project, String operationArea){
        RefundItemAccounting refundItemAccounting = new RefundItemAccounting(
                UUID.randomUUID().toString(),
                refundId,
                orderId,
                priceGross,
                priceNet,
                priceVat,
                companyCode,
                mainLedgerAccount,
                vatCode,
                internalOrder,
                profitCenter,
                balanceProfitCenter,
                project,
                operationArea);

        refundItemAccounting = refundItemAccountingRepository.save(refundItemAccounting);
        toBeDeletedRefundItemAccountingById.add(refundItemAccounting.getRefundItemId());

        return refundItemAccounting;
    }

}

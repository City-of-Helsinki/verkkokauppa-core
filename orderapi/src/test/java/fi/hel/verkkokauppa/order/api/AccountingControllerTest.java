package fi.hel.verkkokauppa.order.api;

import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.data.DummyData;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.accounting.OrderAccounting;
import fi.hel.verkkokauppa.order.model.accounting.OrderItemAccounting;
import fi.hel.verkkokauppa.order.repository.jpa.OrderAccountingRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemAccountingRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    private OrderAccountingRepository orderAccountingRepository;

    @Autowired
    private OrderItemAccountingRepository orderItemAccountingRepository;

    private ArrayList<String> toBeDeletedOrderById = new ArrayList<>();
    private ArrayList<String> toBeDeletedOrderAccountingById = new ArrayList<>();
    private ArrayList<String> toBeDeletedOrderItemAccountingById = new ArrayList<>();

    @After
    public void tearDown() {
        try {
            toBeDeletedOrderById.forEach(orderId -> orderRepository.deleteById(orderId));
            toBeDeletedOrderAccountingById.forEach(id -> orderAccountingRepository.deleteById(id));
            toBeDeletedOrderItemAccountingById.forEach(id -> orderItemAccountingRepository.deleteById(id));
            toBeDeletedOrderById = new ArrayList<>();
            toBeDeletedOrderAccountingById = new ArrayList<>();
            toBeDeletedOrderItemAccountingById = new ArrayList<>();
        } catch (Exception e) {
            log.info("delete error {}", e.toString());
            toBeDeletedOrderById = new ArrayList<>();
            toBeDeletedOrderAccountingById = new ArrayList<>();
            toBeDeletedOrderItemAccountingById = new ArrayList<>();
        }
    }

    @Test
    @RunIfProfile(profile = "local")
    public void testAccountingCreate() throws Exception {
        Order order1 = generateDummyOrder();
        order1.setOrderId(UUID.randomUUID().toString());
        order1 = orderRepository.save(order1);
        toBeDeletedOrderById.add(order1.getOrderId());

        Order order2 = generateDummyOrder();
        order2.setOrderId(UUID.randomUUID().toString());
        order2 = orderRepository.save(order2);
        toBeDeletedOrderById.add(order2.getOrderId());

        OrderAccounting orderAccounting1 = new OrderAccounting();
        orderAccounting1.setOrderId(order1.getOrderId());
        orderAccounting1.setCreatedAt(DateTimeUtil.getFormattedDateTime().minusDays(1));
        orderAccounting1 = orderAccountingRepository.save(orderAccounting1);
        toBeDeletedOrderAccountingById.add(orderAccounting1.getOrderId());

        OrderAccounting orderAccounting2 = new OrderAccounting();
        orderAccounting2.setOrderId(order2.getOrderId());
        orderAccounting2.setCreatedAt(DateTimeUtil.getFormattedDateTime().minusDays(1));
        orderAccounting2 = orderAccountingRepository.save(orderAccounting2);
        toBeDeletedOrderAccountingById.add(orderAccounting2.getOrderId());

        String companyCode1 = "1234";
        OrderItemAccounting orderItemAccounting1 = new OrderItemAccounting(UUID.randomUUID().toString(), order1.getOrderId(), "20", "10", "10", companyCode1, "account", "24", "yes",
                "profitCenter", "project 1", "Area A");
        orderItemAccounting1 = orderItemAccountingRepository.save(orderItemAccounting1);
        toBeDeletedOrderItemAccountingById.add(orderItemAccounting1.getOrderItemId());
        OrderItemAccounting orderItemAccounting2 = new OrderItemAccounting(UUID.randomUUID().toString(), order1.getOrderId(), "10", "5", "5", companyCode1, "account", "24", "yes",
                "profitCenter", "project 2", "Area B");
        orderItemAccounting2 = orderItemAccountingRepository.save(orderItemAccounting2);
        toBeDeletedOrderItemAccountingById.add(orderItemAccounting2.getOrderItemId());
        OrderItemAccounting orderItemAccounting3 = new OrderItemAccounting(UUID.randomUUID().toString(), order1.getOrderId(), "10", "5", "5", companyCode1, "account", "24", "yes",
                "profitCenter", "project 2", "Area B");
        orderItemAccounting3 = orderItemAccountingRepository.save(orderItemAccounting3);
        toBeDeletedOrderItemAccountingById.add(orderItemAccounting3.getOrderItemId());

        String companyCode2 = "5678";
        OrderItemAccounting orderItemAccounting4 = new OrderItemAccounting(UUID.randomUUID().toString(), order2.getOrderId(), "10", "5", "5", companyCode2, "account", "24", "yes",
                "profitCenter", "project 2", "Area B");
        orderItemAccounting4 = orderItemAccountingRepository.save(orderItemAccounting4);
        toBeDeletedOrderItemAccountingById.add(orderItemAccounting4.getOrderItemId());
        OrderItemAccounting orderItemAccounting5 = new OrderItemAccounting(UUID.randomUUID().toString(), order2.getOrderId(), "10", "5", "5", companyCode2, "account", "24", "yes",
                "profitCenter", "project 2", "Area B");
        orderItemAccounting5 = orderItemAccountingRepository.save(orderItemAccounting5);
        toBeDeletedOrderItemAccountingById.add(orderItemAccounting5.getOrderItemId());

        this.mockMvc.perform(
                get("/accounting/create")
            )
            .andDo(print())
            .andExpect(status().is2xxSuccessful())
            .andExpect(status().is(200))
            .andReturn();

        order1 = orderRepository.findById(order1.getOrderId()).orElseThrow();
        assertNotNull(order1.getAccounted());
        order2 = orderRepository.findById(order2.getOrderId()).orElseThrow();
        assertNotNull(order2.getAccounted());
    }
}

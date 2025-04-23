package fi.hel.verkkokauppa.order.api.cron;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.cron.search.dto.PaymentResultDto;
import fi.hel.verkkokauppa.order.api.data.DummyData;
import fi.hel.verkkokauppa.order.api.data.accounting.ProductAccountingDto;
import fi.hel.verkkokauppa.order.constants.RefundAccountingStatusEnum;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderItem;
import fi.hel.verkkokauppa.order.model.accounting.OrderAccounting;
import fi.hel.verkkokauppa.order.model.accounting.OrderItemAccounting;
import fi.hel.verkkokauppa.order.model.accounting.RefundAccounting;
import fi.hel.verkkokauppa.order.model.accounting.RefundItemAccounting;
import fi.hel.verkkokauppa.order.model.refund.Refund;
import fi.hel.verkkokauppa.order.repository.jpa.*;
import fi.hel.verkkokauppa.order.test.utils.TestUtils;
import fi.hel.verkkokauppa.order.test.utils.payment.TestPayment;
import fi.hel.verkkokauppa.order.test.utils.payment.TestRefundPayment;
import fi.hel.verkkokauppa.order.test.utils.productaccounting.TestProductAccounting;
import fi.hel.verkkokauppa.order.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.index.IndexResponse;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.lang.Thread.sleep;
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
public class MissingRefundAccountingFinderControllerTest extends DummyData {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;

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

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestUtils testUtils;

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
        Refund refund1 = createTestRefund(order1.getOrderId());
        Refund refund2 = createTestRefund(order2.getOrderId());
        Refund refund3 = createTestRefund(order3.getOrderId());


        // get number of emails before test
//        int totalMailsBefore = testUtils.mailHoqMessageCount();

        // set order 1 to be accounted
        setAccounted(order1);

        createTestOrderAccounting(order1.getOrderId());
        createTestOrderAccounting(order2.getOrderId());

        String companyCode1 = "1234";
        createTestRefundItemAccounting(
                refund1.getRefundId(),
                order1.getOrderId(),
                "10", "7", "3",
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
        createTestRefundItemAccounting(
                refund1.getRefundId(),
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
        createTestRefundItemAccounting(
                refund1.getRefundId(),
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
        //
        // second company code
        //
        String companyCode2 = "5678";
        createTestRefundItemAccounting(
                refund2.getRefundId(),
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
        createTestRefundItemAccounting(
                refund2.getRefundId(),
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

        // create and save orderitem to db
        OrderItem orderItem = generateDummyOrderItem(order3);
        this.orderItemRepository.save(orderItem);

        order3.setPriceTotal("0.01");
        order3.setStatus("confirmed");
        // Save needed fields to allow query to find it status = confirmed and price total over 0
        orderRepository.save(order3);

        TestRefundPayment refundPayment = new TestRefundPayment();
        refundPayment.setRefundId("test-refund-id" + order3.getOrderId());
        refundPayment.setStatus("refund_created");
        refundPayment.setOrderId(order3.getOrderId());
        refundPayment.setTotal(new BigDecimal(order3.getPriceTotal()));
        IndexResponse testRefundPayment = this.testUtils.createTestRefundPayment(refundPayment);


        ProductAccountingDto accountingDto = createDummyProductAccountingDto(orderItem.getProductId(), order3.getOrderId());
        TestProductAccounting testProductAccounting = objectMapper.convertValue(accountingDto, TestProductAccounting.class);
        testUtils.createTestProductAccounting(testProductAccounting);

        // Define the date-time variables
        LocalDateTime createdAfter = LocalDateTime.now().minusDays(1);
        LocalDateTime createAccountingAfter = LocalDateTime.now().minusDays(1);

        // Format date-time variables as strings
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String createdAfterStr = createdAfter.format(formatter);
        String createAccountingAfterStr = createAccountingAfter.format(formatter);


        log.info("testPaymentOrderId: " + refundPayment.getOrderId());
        MvcResult result = this.mockMvc.perform(
                        get("/accounting/cron/find-missing-accounting")
                                .param("createdAfter", createdAfterStr)
                                .param("createAccountingAfter", createAccountingAfterStr)

                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(status().is(200))
                .andReturn();
        String responseContent = result.getResponse().getContentAsString();
        List<PaymentResultDto> failedToAccount = parseFailedToAccount(responseContent);

        // Find the specific PaymentResultDto by orderId
        PaymentResultDto foundPayment = failedToAccount.stream()
                .filter(paymentResultDto -> Objects.equals(paymentResultDto.getOrderId(), refundPayment.getOrderId()))
                .findFirst()
                .orElse(null); // Returns null if no match is found

        // Assert that the expected PaymentResultDto was found
        assertNotNull(foundPayment, "Response does not contain the expected order ID: " + order3.getOrderId());

        // Assert that order1 and order2 are not in the failedToAccount list
        Order finalOrder1 = order1;
        boolean order1InFailedToAccount = failedToAccount.stream()
                .anyMatch(paymentResultDto -> Objects.equals(paymentResultDto.getOrderId(), finalOrder1.getOrderId()));
        assertFalse(order1InFailedToAccount, "Order 1 should not be in the failedToAccount response");

        Order finalOrder = order2;
        boolean order2InFailedToAccount = failedToAccount.stream()
                .anyMatch(paymentResultDto -> Objects.equals(paymentResultDto.getOrderId(), finalOrder.getOrderId()));
        assertFalse(order2InFailedToAccount, "Order 2 should not be in the failedToAccount response");

        // Ensure order1 marked as accounted
        order1 = orderRepository.findById(order1.getOrderId()).orElseThrow();
        assertNotNull(order1.getAccounted(), "Order 1 should be accounted");

        // mock the process where orderAccounting is created but not yet processed
        order2 = orderRepository.findById(order2.getOrderId()).orElseThrow();
        assertNull(order2.getAccounted(), "Order 2 should be accounted");

        // Ensure order3 is not marked as accounted
        order3 = orderRepository.findById(order3.getOrderId()).orElseThrow();
        assertNull(order3.getAccounted(), "Order 3 should not be accounted");

        // Create orderAccounting to order3
        createTestOrderAccounting(order3.getOrderId());

        // Wait for 3 seconds to acconting to be created
        sleep(3000);
        MvcResult result2 = this.mockMvc.perform(
                        get("/accounting/cron/find-missing-accounting")
                                .param("createdAfter", createdAfterStr)
                                .param("createAccountingAfter", createAccountingAfterStr)
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(status().is(200))
                .andReturn();
        List<PaymentResultDto> failedToAccount2 = parseFailedToAccount(result2.getResponse().getContentAsString());

        Order finalOrder3 = order3;
        boolean order3InFailedToAccount = failedToAccount2.stream()
                .anyMatch(paymentResultDto -> Objects.equals(paymentResultDto.getOrderId(), finalOrder3.getOrderId()));
        assertFalse(order3InFailedToAccount, "Order 3 should not be in the failedToAccount response");


        assertEquals(1, failedToAccount.size(), "Should only be one that is failed to account");

        assertEquals(0, failedToAccount2.size(), "Accounting should be created (if all endpoint)");
        // First, check the size of failedToAccount before and after accounting is created for order3
        assertEquals(
                (failedToAccount.size() - 1),
                failedToAccount2.size(),
                "The size of 'failedToAccount2' should be one less than 'failedToAccount' because 'order3' is now accounted for."
        );

        sleep(5000);

        // Filter messages using the custom condition
        List<JSONObject> totalMailsAfterContainsOrder3 = testUtils.filterMailhoqMessages(mailhoqMessage -> {
            JSONObject content = mailhoqMessage.getJSONObject("Content");
            String body = content.getString("Body");
            return body.contains(finalOrder3.getOrderId());
        });
        assertEquals(1, totalMailsAfterContainsOrder3.size(), "There should be 1 mail that contains order3 orderId");

    }


    private Order createTestOrder() {
        Order order = generateDummyOrder();
        order.setOrderId(UUID.randomUUID().toString());
        order = orderRepository.save(order);
        toBeDeletedOrderById.add(order.getOrderId());
        return order;
    }

    private Order setAccounted(Order order) {
        order.setAccounted(LocalDate.from(DateTimeUtil.getFormattedDateTime().minusDays(1)));
        order = orderRepository.save(order);
        return order;
    }

    private OrderAccounting createTestOrderAccounting(String orderId) {
        OrderAccounting orderAccounting = new OrderAccounting();
        orderAccounting.setOrderId(orderId);
        orderAccounting.setCreatedAt(DateTimeUtil.getFormattedDateTime().minusDays(1));
        orderAccounting.setAccounted(LocalDate.from(DateTimeUtil.getFormattedDateTime().minusDays(1)));
        orderAccounting = orderAccountingRepository.save(orderAccounting);
        toBeDeletedOrderAccountingById.add(orderAccounting.getOrderId());

        return orderAccounting;
    }

    public List<PaymentResultDto> parseFailedToAccount(String jsonString) {
        try {
            return objectMapper.readValue(jsonString, new TypeReference<List<PaymentResultDto>>() {
            });
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>(); // Return an empty list in case of failure
        }
    }

    private OrderItemAccounting createTestOrderItemAccounting(String orderId, String priceGross, String priceNet, String priceVat,
                                                              String companyCode, String mainLedgerAccount, String vatCode,
                                                              String internalOrder, String profitCenter, String balanceProfitCenter,
                                                              String project, String operationArea) {
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
                operationArea,
                LocalDateTime.now(),
                "merchantId",
                "namespace",
                "paytrailTransactionId"
        );

        orderItemAccounting = orderItemAccountingRepository.save(orderItemAccounting);
        toBeDeletedOrderItemAccountingById.add(orderItemAccounting.getOrderItemId());

        return orderItemAccounting;
    }


    private RefundItemAccounting createTestRefundItemAccounting(String refundId, String orderId, String priceGross, String priceNet, String priceVat,
                                                                String companyCode, String mainLedgerAccount, String vatCode,
                                                                String internalOrder, String profitCenter, String balanceProfitCenter,
                                                                String project, String operationArea) {
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
                operationArea,
                LocalDateTime.now(),
                "merchantId",
                "namespace",
                "paytrailTransactionId"
        );

        refundItemAccounting = refundItemAccountingRepository.save(refundItemAccounting);
        toBeDeletedRefundItemAccountingById.add(refundItemAccounting.getRefundItemId());

        return refundItemAccounting;
    }

    private Refund createTestRefund(String orderId) {
        Refund refund = generateDummyRefund(orderId);
        refund.setRefundId(UUID.randomUUID().toString());
        refund = refundRepository.save(refund);
        toBeDeletedRefundById.add(refund.getRefundId());
        return refund;
    }

    private Refund setTestRefundAccountingStatus(String refundId, RefundAccountingStatusEnum accountingStatus) {
        Optional<Refund> returnedRefund = refundRepository.findById(refundId);
        Refund refund = returnedRefund.get();
        if (refund != null) {
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

}

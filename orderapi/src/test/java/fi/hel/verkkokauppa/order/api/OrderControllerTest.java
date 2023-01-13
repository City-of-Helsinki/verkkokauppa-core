package fi.hel.verkkokauppa.order.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.constants.PaymentGatewayEnum;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.order.api.data.DummyData;
import fi.hel.verkkokauppa.order.api.data.OrderPaymentMethodDto;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.OrderPaymentMethod;
import fi.hel.verkkokauppa.order.repository.jpa.OrderPaymentMethodRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
import fi.hel.verkkokauppa.order.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunIfProfile(profile = "local")
@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@Slf4j
public class OrderControllerTest extends DummyData {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderPaymentMethodRepository orderPaymentMethodRepository;

    private ArrayList<String> toBeDeletedOrderById = new ArrayList<>();
    private ArrayList<String> toBeDeletedPaymentMethodByOrderId = new ArrayList<>();

    @After
    public void tearDown() {
        try {
            toBeDeletedOrderById.forEach(orderId -> orderRepository.deleteById(orderId));
            toBeDeletedPaymentMethodByOrderId.forEach(paymentMethodId -> orderPaymentMethodRepository.deleteByOrderId(paymentMethodId));
            // Clear list because all deleted
            toBeDeletedOrderById = new ArrayList<>();
            toBeDeletedPaymentMethodByOrderId = new ArrayList<>();
        } catch (Exception e) {
            log.info("delete error {}", e.toString());
            toBeDeletedOrderById = new ArrayList<>();
            toBeDeletedPaymentMethodByOrderId = new ArrayList<>();
        }
    }

    @Test
    @RunIfProfile(profile = "local")
    public void whenUpsertPaymentMethodWithValidDataThenReturnStatus201() throws Exception {
        Order order = generateDummyOrder();
        order.setOrderId(UUID.randomUUID().toString());
        order.setNamespace("venepaikat");
        order = orderRepository.save(order);

        // First test creation
        String testOrderId = order.getOrderId();
        String testUserId = order.getUser();

        List<OrderPaymentMethod> existingPaymentMethodsForOrder = orderPaymentMethodRepository.findByOrderId(testOrderId);
        assertEquals(0, existingPaymentMethodsForOrder.size());

        OrderPaymentMethodDto dto1 = createTestOrderPaymentMethodDto(testOrderId, testUserId, PaymentGatewayEnum.PAYTRAIL);

        MvcResult response1 = this.mockMvc.perform(
                        post("/order/setPaymentMethod")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(dto1))
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(status().is(201))
                .andReturn();

        existingPaymentMethodsForOrder = orderPaymentMethodRepository.findByOrderId(testOrderId);
        assertEquals(1, existingPaymentMethodsForOrder.size());

        OrderPaymentMethodDto newPaymentMethodDto = mapper.readValue(response1.getResponse().getContentAsString(), OrderPaymentMethodDto.class);
        assertNotNull(newPaymentMethodDto);
        assertEquals(testOrderId, newPaymentMethodDto.getOrderId());
        assertNotNull(newPaymentMethodDto.getName());
        assertNotNull(newPaymentMethodDto.getGroup());
        assertNotNull(newPaymentMethodDto.getImg());
        assertNotNull(newPaymentMethodDto.getGateway());

        assertEquals(testOrderId, newPaymentMethodDto.getOrderId());
        assertEquals(testUserId, newPaymentMethodDto.getUserId());
        assertEquals("Test payment method", newPaymentMethodDto.getName());
        assertEquals("test-payment-code", newPaymentMethodDto.getCode());
        assertEquals("test-payment-group", newPaymentMethodDto.getGroup());
        assertEquals("test-payment.jpg", newPaymentMethodDto.getImg());
        assertEquals(PaymentGatewayEnum.PAYTRAIL, newPaymentMethodDto.getGateway());


        // Then test updating when payment method already exists for provided orderId
        OrderPaymentMethodDto dto2 = createTestOrderPaymentMethodDto(testOrderId, testUserId, PaymentGatewayEnum.VISMA);
        dto2.setName("Edited payment method");
        dto2.setCode("edited-payment-code");
        dto2.setGroup("edit-group");
        dto2.setImg("edit-method.jpg");

        MvcResult response2 = this.mockMvc.perform(
                        post("/order/setPaymentMethod")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(dto2))
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(status().is(201))
                .andReturn();

        existingPaymentMethodsForOrder = orderPaymentMethodRepository.findByOrderId(testOrderId);
        assertEquals(1, existingPaymentMethodsForOrder.size());

        OrderPaymentMethodDto updatedPaymentMethodDto = mapper.readValue(response2.getResponse().getContentAsString(), OrderPaymentMethodDto.class);
        assertNotNull(updatedPaymentMethodDto);
        assertEquals(testOrderId, updatedPaymentMethodDto.getOrderId());
        assertNotNull(updatedPaymentMethodDto.getName());
        assertNotNull(updatedPaymentMethodDto.getGroup());
        assertNotNull(updatedPaymentMethodDto.getImg());
        assertNotNull(updatedPaymentMethodDto.getGateway());

        assertEquals(testOrderId, updatedPaymentMethodDto.getOrderId());
        assertEquals(testUserId, updatedPaymentMethodDto.getUserId());
        assertEquals("Edited payment method", updatedPaymentMethodDto.getName());
        assertEquals("edited-payment-code", updatedPaymentMethodDto.getCode());
        assertEquals("edit-group", updatedPaymentMethodDto.getGroup());
        assertEquals("edit-method.jpg", updatedPaymentMethodDto.getImg());
        assertEquals(PaymentGatewayEnum.VISMA, updatedPaymentMethodDto.getGateway());

        toBeDeletedOrderById.add(testOrderId);
        toBeDeletedPaymentMethodByOrderId.add(testOrderId);
    }

    @Test
    @RunIfProfile(profile = "local")
    public void whenUpsertPaymentMethodWithInvalidUserIdThenReturnStatus400() throws Exception {
        Order order = generateDummyOrder();
        order.setOrderId(UUID.randomUUID().toString());
        order.setNamespace("venepaikat");
        order = orderRepository.save(order);

        // First test creation
        String testOrderId = order.getOrderId();
        String invalidUserId = "wrong-user";

        List<OrderPaymentMethod> existingPaymentMethodsForOrder = orderPaymentMethodRepository.findByOrderId(testOrderId);
        assertEquals(0, existingPaymentMethodsForOrder.size());

        OrderPaymentMethodDto dto = createTestOrderPaymentMethodDto(testOrderId, invalidUserId, PaymentGatewayEnum.PAYTRAIL);
        this.mockMvc.perform(
                        post("/order/setPaymentMethod")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(dto))
                )
                .andDo(print())
                .andExpect(status().is4xxClientError())
                .andExpect(status().is(404))
                .andReturn();
        toBeDeletedOrderById.add(testOrderId);
    }

    private OrderPaymentMethodDto createTestOrderPaymentMethodDto(String orderId, String userId, PaymentGatewayEnum gateway) {
        return new OrderPaymentMethodDto(orderId,
                userId,
                "Test payment method",
                "test-payment-code",
                "test-payment-group",
                "test-payment.jpg",
                gateway);
    }
}

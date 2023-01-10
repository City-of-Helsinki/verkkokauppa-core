package fi.hel.verkkokauppa.payment.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.payment.api.data.OrderPaymentMethodDto;
import fi.hel.verkkokauppa.payment.constant.PaymentGatewayEnum;
import fi.hel.verkkokauppa.payment.mapper.OrderPaymentMethodMapper;
import fi.hel.verkkokauppa.payment.model.OrderPaymentMethod;
import fi.hel.verkkokauppa.payment.repository.OrderPaymentMethodRepository;
import fi.hel.verkkokauppa.payment.service.PaymentMethodService;
import fi.hel.verkkokauppa.payment.testing.utils.AutoMockBeanFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentMethodController.class) // Change and uncomment
@Import(PaymentMethodController.class) // Change and uncomment
@ContextConfiguration(classes = AutoMockBeanFactory.class) // This automatically mocks missing beans
@AutoConfigureMockMvc // This activates auto configuration to call mocked api endpoints.
@Slf4j
@EnableAutoConfiguration(exclude = {
        ActiveMQAutoConfiguration.class,
        KafkaAutoConfiguration.class
})
public class PaymentMethodControllerUnitTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    // You need to add all dependencies in controller with @Autowired annotation
    // as new field with @MockBean to controller test.
    @MockBean
    private PaymentMethodService paymentMethodService;

    @MockBean
    private OrderPaymentMethodRepository orderPaymentMethodRepository;

    @MockBean
    private OrderPaymentMethodMapper orderPaymentMethodMapper;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(paymentMethodService, "orderPaymentMethodRepository", orderPaymentMethodRepository);
        ReflectionTestUtils.setField(paymentMethodService, "orderPaymentMethodMapper", orderPaymentMethodMapper);
    }

    @Test
    public void whenUpsertCreateNewPaymentMethodWithValidDataThenReturnStatus201() throws Exception {
        String testOrderId = "order-id-123";
        OrderPaymentMethodDto dto = createTestOrderPaymentMethodDto(testOrderId, PaymentGatewayEnum.PAYTRAIL);
        OrderPaymentMethod model = mapper.convertValue(dto, OrderPaymentMethod.class);

        Mockito.when(paymentMethodService.upsertOrderPaymentMethod(any(OrderPaymentMethodDto.class))).thenCallRealMethod();
        Mockito.when(orderPaymentMethodRepository.findByOrderId(dto.getOrderId())).thenReturn(Collections.emptyList());
        Mockito.when(orderPaymentMethodRepository.save(model)).thenReturn(model);
        Mockito.when(orderPaymentMethodMapper.fromDto(dto)).thenReturn(model);
        Mockito.when(orderPaymentMethodMapper.toDto(model)).thenReturn(dto);

        MvcResult response = this.mockMvc.perform(
                        post("/paymentmethod/order/setPaymentMethod")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(dto))
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(status().is(201))
                .andReturn();
        OrderPaymentMethodDto responseDto = mapper.readValue(response.getResponse().getContentAsString(), OrderPaymentMethodDto.class);
        assertNotNull(responseDto);
        assertEquals(responseDto, dto);

        assertEquals(testOrderId, responseDto.getOrderId());
        assertEquals("dummy-user", responseDto.getUserId());
        assertEquals("Test payment method", responseDto.getName());
        assertEquals("test-payment-code", responseDto.getCode());
        assertEquals("test-payment-group", responseDto.getGroup());
        assertEquals("test-payment.jpg", responseDto.getImg());
        assertEquals(PaymentGatewayEnum.PAYTRAIL, responseDto.getGateway());
    }

    @Test
    public void whenUpsertUpdatePaymentMethodWithValidDataThenReturnStatus201() throws Exception {
        String testOrderId = "order-id-123";
        // Existing method mock
        OrderPaymentMethodDto existingMethodMockDto = createTestOrderPaymentMethodDto(testOrderId, PaymentGatewayEnum.PAYTRAIL);
        OrderPaymentMethod existingMethodMock = mapper.convertValue(existingMethodMockDto, OrderPaymentMethod.class);

        // Updated method mock
        OrderPaymentMethodDto updatedMethodMockDto = createTestOrderPaymentMethodDto(testOrderId, PaymentGatewayEnum.VISMA);
        updatedMethodMockDto.setName("Edited payment method");
        updatedMethodMockDto.setCode("edited-payment-code");
        updatedMethodMockDto.setGroup("edit-group");
        updatedMethodMockDto.setImg("edit-method.jpg");
        OrderPaymentMethod updatedMethodMock = mapper.convertValue(updatedMethodMockDto, OrderPaymentMethod.class);

        Mockito.when(paymentMethodService.upsertOrderPaymentMethod(any(OrderPaymentMethodDto.class))).thenCallRealMethod();
        Mockito.when(orderPaymentMethodRepository.findByOrderId(testOrderId)).thenReturn(Arrays.asList(existingMethodMock));
        Mockito.when(orderPaymentMethodRepository.save(any(OrderPaymentMethod.class))).thenReturn(updatedMethodMock);
        Mockito.when(orderPaymentMethodMapper.updateFromDtoToModel(any(OrderPaymentMethod.class), any(OrderPaymentMethodDto.class))).thenReturn(updatedMethodMock);
        Mockito.when(orderPaymentMethodMapper.toDto(any(OrderPaymentMethod.class))).thenReturn(updatedMethodMockDto);

        MvcResult response = this.mockMvc.perform(
                        post("/paymentmethod/order/setPaymentMethod")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(updatedMethodMockDto))
                )
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(status().is(201))
                .andReturn();
        OrderPaymentMethodDto responseDto = mapper.readValue(response.getResponse().getContentAsString(), OrderPaymentMethodDto.class);
        assertNotNull(responseDto);
        assertEquals(responseDto, updatedMethodMockDto);

        assertEquals(testOrderId, responseDto.getOrderId());
        assertEquals("dummy-user", responseDto.getUserId());
        assertEquals("Edited payment method", responseDto.getName());
        assertEquals("edited-payment-code", responseDto.getCode());
        assertEquals("edit-group", responseDto.getGroup());
        assertEquals("edit-method.jpg", responseDto.getImg());
        assertEquals(PaymentGatewayEnum.VISMA, responseDto.getGateway());
    }

    private OrderPaymentMethodDto createTestOrderPaymentMethodDto(String orderId, PaymentGatewayEnum gateway) {
        return new OrderPaymentMethodDto(orderId,
                "dummy-user",
                "Test payment method",
                "test-payment-code",
                "test-payment-group",
                "test-payment.jpg",
                gateway);
    }


}

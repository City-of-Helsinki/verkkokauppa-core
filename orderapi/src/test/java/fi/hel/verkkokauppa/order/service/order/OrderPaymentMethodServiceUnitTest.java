package fi.hel.verkkokauppa.order.service.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.constants.PaymentGatewayEnum;
import fi.hel.verkkokauppa.order.api.data.OrderPaymentMethodDto;
import fi.hel.verkkokauppa.order.mapper.OrderPaymentMethodMapper;
import fi.hel.verkkokauppa.order.model.OrderPaymentMethod;
import fi.hel.verkkokauppa.order.repository.jpa.OrderPaymentMethodRepository;
import fi.hel.verkkokauppa.order.testing.annotations.UnitTest;
import fi.hel.verkkokauppa.order.testing.utils.AutoMockBeanFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(SpringExtension.class)
@UnitTest
@WebMvcTest(OrderPaymentMethodService.class)
@ContextConfiguration(classes = AutoMockBeanFactory.class) // This automatically mocks missing beans
@EnableAutoConfiguration(exclude = {
        ActiveMQAutoConfiguration.class,
        KafkaAutoConfiguration.class
})
public class OrderPaymentMethodServiceUnitTest {

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private OrderPaymentMethodService orderPaymentMethodService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private OrderPaymentMethodRepository orderPaymentMethodRepository;

    @MockBean
    private OrderPaymentMethodMapper orderPaymentMethodMapper;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(orderPaymentMethodService, "orderService", orderService);
        ReflectionTestUtils.setField(orderPaymentMethodService, "orderPaymentMethodRepository", orderPaymentMethodRepository);
        ReflectionTestUtils.setField(orderPaymentMethodService, "orderPaymentMethodMapper", orderPaymentMethodMapper);
    }

    @Test
    public void whenUpsertCreateNewPaymentMethodWithValidDataThenReturnStatus201() {
        String testOrderId = "order-id-123";
        OrderPaymentMethodDto dto = createTestOrderPaymentMethodDto(testOrderId, PaymentGatewayEnum.PAYTRAIL);
        OrderPaymentMethod model = mapper.convertValue(dto, OrderPaymentMethod.class);

        Mockito.when(orderPaymentMethodService.upsertOrderPaymentMethod(any(OrderPaymentMethodDto.class))).thenCallRealMethod();
        Mockito.when(orderPaymentMethodRepository.findByOrderId(dto.getOrderId())).thenReturn(Collections.emptyList());
        Mockito.when(orderPaymentMethodRepository.save(model)).thenReturn(model);
        Mockito.when(orderPaymentMethodMapper.fromDto(dto)).thenReturn(model);
        Mockito.when(orderPaymentMethodMapper.toDto(model)).thenReturn(dto);

        OrderPaymentMethodDto responseDto = orderPaymentMethodService.upsertOrderPaymentMethod(dto);
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

        Mockito.when(orderPaymentMethodService.upsertOrderPaymentMethod(any(OrderPaymentMethodDto.class))).thenCallRealMethod();
        Mockito.when(orderPaymentMethodRepository.findByOrderId(testOrderId)).thenReturn(Arrays.asList(existingMethodMock));
        Mockito.when(orderPaymentMethodRepository.save(any(OrderPaymentMethod.class))).thenReturn(updatedMethodMock);
        Mockito.when(orderPaymentMethodMapper.updateFromDtoToModel(any(OrderPaymentMethod.class), any(OrderPaymentMethodDto.class))).thenReturn(updatedMethodMock);
        Mockito.when(orderPaymentMethodMapper.toDto(any(OrderPaymentMethod.class))).thenReturn(updatedMethodMockDto);

        OrderPaymentMethodDto responseDto = orderPaymentMethodService.upsertOrderPaymentMethod(updatedMethodMockDto);
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

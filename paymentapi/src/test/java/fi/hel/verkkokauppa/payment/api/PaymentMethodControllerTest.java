package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.payment.api.data.OrderPaymentMethodDto;
import fi.hel.verkkokauppa.payment.constant.PaymentGatewayEnum;
import fi.hel.verkkokauppa.payment.model.OrderPaymentMethod;
import fi.hel.verkkokauppa.payment.repository.OrderPaymentMethodRepository;
import fi.hel.verkkokauppa.payment.testing.BaseFunctionalTest;
import fi.hel.verkkokauppa.payment.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class PaymentMethodControllerTest extends BaseFunctionalTest {

    @Autowired
    private PaymentMethodController paymentMethodController;

    @Autowired
    private OrderPaymentMethodRepository orderPaymentMethodRepository;

    private ArrayList<String> toBeDeletedPaymentMethodByOrderId = new ArrayList<>();

    @After
    public void tearDown() {
        try {
            toBeDeletedPaymentMethodByOrderId.forEach(paymentMethodId -> {
                orderPaymentMethodRepository.deleteByOrderId(paymentMethodId);
            });
            // Clear list because all deleted
            toBeDeletedPaymentMethodByOrderId = new ArrayList<>();
        } catch (Exception e) {
            log.info("delete error {}", e.toString());
            toBeDeletedPaymentMethodByOrderId = new ArrayList<>();
        }
    }

    @Test
    @RunIfProfile(profile = "local")
    public void whenUpsertPaymentMethodWithValidDataThenReturnStatus201() {
        // First test creation
        String testOrderId = "order-id-123";

        List<OrderPaymentMethod> existingPaymentMethodsForOrder = orderPaymentMethodRepository.findByOrderId(testOrderId);
        assertEquals(0, existingPaymentMethodsForOrder.size());

        OrderPaymentMethodDto dto1 = createTestOrderPaymentMethodDto(testOrderId, PaymentGatewayEnum.PAYTRAIL);
        ResponseEntity<OrderPaymentMethodDto> response1 = paymentMethodController.upsertOrderPaymentMethod(dto1);
        assertEquals(HttpStatus.CREATED, response1.getStatusCode());

        existingPaymentMethodsForOrder = orderPaymentMethodRepository.findByOrderId(testOrderId);
        assertEquals(1, existingPaymentMethodsForOrder.size());

        OrderPaymentMethodDto newPaymentMethodDto = response1.getBody();
        assertNotNull(newPaymentMethodDto);
        assertEquals(testOrderId, newPaymentMethodDto.getOrderId());
        assertNotNull(newPaymentMethodDto.getName());
        assertNotNull(newPaymentMethodDto.getGroup());
        assertNotNull(newPaymentMethodDto.getImg());
        assertNotNull(newPaymentMethodDto.getGateway());

        assertEquals(testOrderId, newPaymentMethodDto.getOrderId());
        assertEquals("dummy-user", newPaymentMethodDto.getUserId());
        assertEquals("Test payment method", newPaymentMethodDto.getName());
        assertEquals("test-payment-code", newPaymentMethodDto.getCode());
        assertEquals("test-payment-group", newPaymentMethodDto.getGroup());
        assertEquals("test-payment.jpg", newPaymentMethodDto.getImg());
        assertEquals(PaymentGatewayEnum.PAYTRAIL, newPaymentMethodDto.getGateway());


        // Then test updating when payment method already exists for provided orderId
        OrderPaymentMethodDto dto2 = createTestOrderPaymentMethodDto(testOrderId, PaymentGatewayEnum.VISMA);
        dto2.setName("Edited payment method");
        dto2.setCode("edited-payment-code");
        dto2.setGroup("edit-group");
        dto2.setImg("edit-method.jpg");
        ResponseEntity<OrderPaymentMethodDto> response2 = paymentMethodController.upsertOrderPaymentMethod(dto2);
        assertEquals(HttpStatus.CREATED, response2.getStatusCode());

        existingPaymentMethodsForOrder = orderPaymentMethodRepository.findByOrderId(testOrderId);
        assertEquals(1, existingPaymentMethodsForOrder.size());

        OrderPaymentMethodDto updatedPaymentMethodDto = response2.getBody();
        assertNotNull(updatedPaymentMethodDto);
        assertEquals(testOrderId, updatedPaymentMethodDto.getOrderId());
        assertNotNull(updatedPaymentMethodDto.getName());
        assertNotNull(updatedPaymentMethodDto.getGroup());
        assertNotNull(updatedPaymentMethodDto.getImg());
        assertNotNull(updatedPaymentMethodDto.getGateway());

        assertEquals(testOrderId, updatedPaymentMethodDto.getOrderId());
        assertEquals("dummy-user", updatedPaymentMethodDto.getUserId());
        assertEquals("Edited payment method", updatedPaymentMethodDto.getName());
        assertEquals("edited-payment-code", updatedPaymentMethodDto.getCode());
        assertEquals("edit-group", updatedPaymentMethodDto.getGroup());
        assertEquals("edit-method.jpg", updatedPaymentMethodDto.getImg());
        assertEquals(PaymentGatewayEnum.VISMA, updatedPaymentMethodDto.getGateway());

        toBeDeletedPaymentMethodByOrderId.add(testOrderId);
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

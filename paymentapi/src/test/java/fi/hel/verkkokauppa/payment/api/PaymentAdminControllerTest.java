package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.payment.api.data.PaymentMethodDto;
import fi.hel.verkkokauppa.payment.constant.GatewayEnum;
import fi.hel.verkkokauppa.payment.model.PaymentMethod;
import fi.hel.verkkokauppa.payment.repository.PaymentMethodRepository;
import fi.hel.verkkokauppa.payment.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Slf4j
public class PaymentAdminControllerTest {

    private ArrayList<String> toBeDeleted = new ArrayList<>();

    @Autowired
    private PaymentAdminController paymentAdminController;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @AfterEach
    void tearDown() {
        try {
            toBeDeleted.forEach(s -> paymentMethodRepository.deleteByCode(s));
            // Clear list because all merchants deleted
            toBeDeleted = new ArrayList<>();
        } catch (Exception e) {
            log.info("delete error {}", e.toString());
        }
    }


    /*
     * It tests the create payment method endpoint.
     */
    @Test
    @RunIfProfile(profile = "local")
    public void whenCreatePaymentMethodWithValidData_thenReturnStatus201() {
        PaymentMethodDto paymentMethodDto = createTestPaymentMethodDto(GatewayEnum.ONLINE);
        ResponseEntity<PaymentMethodDto> response = paymentAdminController.createPaymentMethod(paymentMethodDto);

        PaymentMethodDto responsePaymentMethodDto = response.getBody();
        Assertions.assertNotNull(responsePaymentMethodDto);
        String code = responsePaymentMethodDto.getCode();
        toBeDeleted.add(code);
        Assertions.assertNotNull(code);
        Assertions.assertNotNull(responsePaymentMethodDto.getName());
        Assertions.assertNotNull(responsePaymentMethodDto.getGroup());
        Assertions.assertNotNull(responsePaymentMethodDto.getImg());
        Assertions.assertNotNull(responsePaymentMethodDto.getGateway());

        Assertions.assertEquals("Test payment method", responsePaymentMethodDto.getName());
        Assertions.assertEquals("test-payment-code", responsePaymentMethodDto.getCode());
        Assertions.assertEquals("test-payment-group", responsePaymentMethodDto.getGroup());
        Assertions.assertEquals("test-payment.jpg", responsePaymentMethodDto.getImg());
        Assertions.assertEquals(GatewayEnum.ONLINE, responsePaymentMethodDto.getGateway());
    }

    @Test
    @RunIfProfile(profile = "local")
    public void whenCreatePaymentMethodWithSameCodeThatExists_thenReturnError409() {
        setupInitialData();
        paymentMethodRepository.findAll().forEach(method -> log.info(method.getCode()));
        PaymentMethodDto paymentMethodDto = createTestPaymentMethodDto(GatewayEnum.ONLINE);

        CommonApiException exception = assertThrows(CommonApiException.class, () -> {
            paymentAdminController.createPaymentMethod(paymentMethodDto);
        });

        assertEquals(CommonApiException.class, exception.getClass());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("payment-method-already-exists", exception.getErrors().getErrors().get(0).getCode());
        assertEquals("payment method with code [test-payment-code] already exists", exception.getErrors().getErrors().get(0).getMessage());
    }


    /*
     * It tests the update payment method endpoint.
     */
    @Test
    @RunIfProfile(profile = "local")
    public void whenUpdatePaymentMethodWithValidData_thenReturnStatus200() {
        setupInitialData();

        String code = "test-payment-code";
        PaymentMethodDto updatedPaymentMethodDto = createTestPaymentMethodDto(GatewayEnum.OFFLINE);
        updatedPaymentMethodDto.setName("Edited test payment method");

        ResponseEntity<PaymentMethodDto> response = paymentAdminController.updatePaymentMethod(code, updatedPaymentMethodDto);

        PaymentMethodDto responsePaymentMethodDto = response.getBody();
        Assertions.assertNotNull(responsePaymentMethodDto);
        String resCode = responsePaymentMethodDto.getCode();
        toBeDeleted.add(resCode);
        Assertions.assertNotNull(code);
        Assertions.assertNotNull(responsePaymentMethodDto.getName());
        Assertions.assertNotNull(responsePaymentMethodDto.getGroup());
        Assertions.assertNotNull(responsePaymentMethodDto.getImg());
        Assertions.assertNotNull(responsePaymentMethodDto.getGateway());

        Assertions.assertEquals("Edited test payment method", responsePaymentMethodDto.getName());
        Assertions.assertEquals("test-payment-code", responsePaymentMethodDto.getCode());
        Assertions.assertEquals("test-payment-group", responsePaymentMethodDto.getGroup());
        Assertions.assertEquals("test-payment.jpg", responsePaymentMethodDto.getImg());
        Assertions.assertEquals(GatewayEnum.OFFLINE, responsePaymentMethodDto.getGateway());
    }

    @Test
    @RunIfProfile(profile = "local")
    public void whenUpdatePaymentMethodThatDoesNotExist_thenReturnError404() {
        PaymentMethodDto paymentMethodDto = createTestPaymentMethodDto(GatewayEnum.ONLINE);

        CommonApiException exception = assertThrows(CommonApiException.class, () -> {
            paymentAdminController.updatePaymentMethod(paymentMethodDto.getCode(), paymentMethodDto);
        });

        assertEquals(CommonApiException.class, exception.getClass());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("payment-method-not-found", exception.getErrors().getErrors().get(0).getCode());
        assertEquals("payment method with code [test-payment-code] not found", exception.getErrors().getErrors().get(0).getMessage());
    }

    /*
     * It tests the delete payment method endpoint.
     */
    @Test
    @RunIfProfile(profile = "local")
    public void whenDeletePaymentMethodThatExists_thenReturnStatus200() {
        setupInitialData();

        String code = "test-payment-code";
        ResponseEntity<String> response = paymentAdminController.deletePaymentMethod(code);
        String responseCode = response.getBody();
        Assertions.assertNotNull(responseCode);
        assertEquals(code, responseCode);

    }

    @Test
    @RunIfProfile(profile = "local")
    public void whenDeletePaymentMethodThatDoesNotExist_thenReturnError404() {
        String code = "test-payment-code";
        CommonApiException exception = assertThrows(CommonApiException.class, () -> {
            paymentAdminController.deletePaymentMethod(code);
        });

        assertEquals(CommonApiException.class, exception.getClass());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("payment-method-not-found", exception.getErrors().getErrors().get(0).getCode());
        assertEquals("payment method with code [test-payment-code] not found", exception.getErrors().getErrors().get(0).getMessage());
    }


    /*
     * It tests the get payment method by code endpoint.
     */
    @Test
    @RunIfProfile(profile = "local")
    public void whenGetPaymentMethodByCodeThatDoesNotExist_thenReturnStatus200() {
        setupInitialData();

        String code = "test-payment-code";
        ResponseEntity<PaymentMethodDto> response = paymentAdminController.getPaymentMethodByCode(code);

        PaymentMethodDto responsePaymentMethodDto = response.getBody();
        Assertions.assertNotNull(responsePaymentMethodDto);
        String resCode = responsePaymentMethodDto.getCode();
        Assertions.assertNotNull(resCode);
        toBeDeleted.add(resCode);
        Assertions.assertNotNull(responsePaymentMethodDto.getName());
        Assertions.assertNotNull(responsePaymentMethodDto.getGroup());
        Assertions.assertNotNull(responsePaymentMethodDto.getImg());
        Assertions.assertNotNull(responsePaymentMethodDto.getGateway());

        Assertions.assertEquals("Test payment method", responsePaymentMethodDto.getName());
        Assertions.assertEquals("test-payment-code", responsePaymentMethodDto.getCode());
        Assertions.assertEquals("test-payment-group", responsePaymentMethodDto.getGroup());
        Assertions.assertEquals("test-payment.jpg", responsePaymentMethodDto.getImg());
        Assertions.assertEquals(GatewayEnum.ONLINE, responsePaymentMethodDto.getGateway());
    }

    @Test
    @RunIfProfile(profile = "local")
    public void whenGetPaymentMethodByCodeThatDoesNotExist_thenReturnError404() {
        String code = "test-payment-code";
        CommonApiException exception = assertThrows(CommonApiException.class, () -> {
            paymentAdminController.getPaymentMethodByCode(code);
        });

        assertEquals(CommonApiException.class, exception.getClass());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("payment-method-not-found", exception.getErrors().getErrors().get(0).getCode());
        assertEquals("payment method with code [test-payment-code] not found", exception.getErrors().getErrors().get(0).getMessage());
    }

    /*
     * It tests the get all payment methods endpoint.
     */
    @Test
    @RunIfProfile(profile = "local")
    public void whenGetPaymentMethods_thenReturnMoreThanZeroWithStatus200() {
        setupInitialData();

        ResponseEntity<List<PaymentMethodDto>> response = paymentAdminController.getPaymentMethods();

        List<PaymentMethodDto> responsePaymentMethodDtos = response.getBody();
        Assertions.assertNotNull(responsePaymentMethodDtos);
        Assertions.assertTrue(responsePaymentMethodDtos.size() > 0);

        List<PaymentMethodDto> responseTestDataPaymentMethodDtos = responsePaymentMethodDtos.stream()
                .filter(pm -> pm.getCode().startsWith("test-"))
                .collect(Collectors.toList());
        Assertions.assertEquals(responseTestDataPaymentMethodDtos.size(), 1);

        PaymentMethodDto responsePaymentMethodDto = responseTestDataPaymentMethodDtos.get(0);
        Assertions.assertNotNull(responsePaymentMethodDto);
        String resCode = responsePaymentMethodDto.getCode();
        Assertions.assertNotNull(resCode);
        toBeDeleted.add(resCode);
        Assertions.assertNotNull(responsePaymentMethodDto.getName());
        Assertions.assertNotNull(responsePaymentMethodDto.getGroup());
        Assertions.assertNotNull(responsePaymentMethodDto.getImg());
        Assertions.assertNotNull(responsePaymentMethodDto.getGateway());

        Assertions.assertEquals("Test payment method", responsePaymentMethodDto.getName());
        Assertions.assertEquals("test-payment-code", responsePaymentMethodDto.getCode());
        Assertions.assertEquals("test-payment-group", responsePaymentMethodDto.getGroup());
        Assertions.assertEquals("test-payment.jpg", responsePaymentMethodDto.getImg());
        Assertions.assertEquals(GatewayEnum.ONLINE, responsePaymentMethodDto.getGateway());
    }

    private PaymentMethodDto createTestPaymentMethodDto(GatewayEnum gateway) {
        return new PaymentMethodDto("Test payment method",
                "test-payment-code",
                "test-payment-group",
                "test-payment.jpg",
                gateway);
    }

    private void setupInitialData() {
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setName("Test payment method");
        paymentMethod.setCode("test-payment-code");
        paymentMethod.setGroup("test-payment-group");
        paymentMethod.setImg("test-payment.jpg");
        paymentMethod.setGateway(GatewayEnum.ONLINE);
        PaymentMethod saved = paymentMethodRepository.save(paymentMethod);
        toBeDeleted.add(saved.getCode());
    }

}

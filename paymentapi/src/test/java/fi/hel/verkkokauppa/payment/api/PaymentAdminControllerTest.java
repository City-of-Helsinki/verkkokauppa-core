package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.payment.api.data.PaymentFilterDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentMethodDto;
import fi.hel.verkkokauppa.payment.constant.GatewayEnum;
import fi.hel.verkkokauppa.payment.model.PaymentFilter;
import fi.hel.verkkokauppa.payment.model.PaymentMethodModel;
import fi.hel.verkkokauppa.payment.repository.PaymentFilterRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentMethodRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@RunIfProfile(profile = "local")
@SpringBootTest
@Slf4j
public class PaymentAdminControllerTest {

    private ArrayList<String> paymentMethodsToBeDeleted = new ArrayList<>();
    private List<String> paymentFiltersToBeDeleted = new ArrayList<>();

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private PaymentAdminController paymentAdminController;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private PaymentFilterRepository paymentFilterRepository;

    @AfterEach
    void tearDown() {
        try {
            paymentMethodsToBeDeleted.forEach(s -> paymentMethodRepository.deleteByCode(s));
            // Clear list because all merchants deleted
            paymentMethodsToBeDeleted = new ArrayList<>();
        } catch (Exception e) {
            log.info("delete error {}", e.toString());
        }
        try {
            paymentFiltersToBeDeleted.forEach(s -> paymentFilterRepository.deleteById(s));
            // Clear list because all filters deleted
            paymentFiltersToBeDeleted = new ArrayList<>();
        } catch (Exception e) {
            log.info("delete error {}", e.toString());
        }
    }

    @Test
    @RunIfProfile(profile = "local")
    public void savePaymentFilter() throws JsonProcessingException {
        List<PaymentFilterDto> request = new ArrayList<>();
        PaymentFilterDto orderPaymentFilterForNordea = new PaymentFilterDto();
        orderPaymentFilterForNordea.setReferenceId("order-1");
        orderPaymentFilterForNordea.setReferenceType("order");
        orderPaymentFilterForNordea.setNamespace("testi-namespace");
        orderPaymentFilterForNordea.setFilterType("banks");
        orderPaymentFilterForNordea.setValue("nordea");

        PaymentFilterDto orderPaymentFilterForOp = new PaymentFilterDto();
        orderPaymentFilterForOp.setReferenceId("order-1");
        orderPaymentFilterForOp.setReferenceType("order");
        orderPaymentFilterForOp.setNamespace("testi-namespace");
        orderPaymentFilterForOp.setFilterType("banks");
        orderPaymentFilterForOp.setValue("op");

        PaymentFilterDto merchantPaymentFilterForNordea = new PaymentFilterDto();
        merchantPaymentFilterForNordea.setReferenceId("merchant-1");
        merchantPaymentFilterForNordea.setReferenceType("merchant");
        merchantPaymentFilterForNordea.setNamespace("testi-namespace");
        merchantPaymentFilterForNordea.setFilterType("banks");
        merchantPaymentFilterForNordea.setValue("nordea");

        PaymentFilterDto merchantPaymentFilterForOp = new PaymentFilterDto();
        merchantPaymentFilterForOp.setReferenceId("merchant-1");
        merchantPaymentFilterForOp.setReferenceType("merchant");
        merchantPaymentFilterForOp.setNamespace("testi-namespace");
        merchantPaymentFilterForOp.setFilterType("banks");
        merchantPaymentFilterForOp.setValue("op");

        request.add(orderPaymentFilterForNordea);
        request.add(orderPaymentFilterForOp);
        request.add(merchantPaymentFilterForNordea);
        request.add(merchantPaymentFilterForOp);

        ResponseEntity<List<PaymentFilterDto>> response = paymentAdminController.savePaymentFilters(request);
        log.info("response : {}",mapper.writeValueAsString(response.getBody()));
        Assertions.assertEquals(response.getStatusCode(), HttpStatus.CREATED);
        PaymentFilterDto firstExpectedFilterDto = Objects.requireNonNull(response.getBody())
                .stream()
                .filter(paymentFilterDto ->
                        Objects.equals(orderPaymentFilterForNordea.getReferenceType(), paymentFilterDto.getReferenceType()) &&
                                Objects.equals(orderPaymentFilterForNordea.getValue(), paymentFilterDto.getValue())
                ).collect(Collectors.toList()).get(0);

        PaymentFilterDto secondExpectedFilterDto = Objects.requireNonNull(response.getBody())
                .stream()
                .filter(paymentFilterDto ->
                        Objects.equals(orderPaymentFilterForOp.getReferenceType(), paymentFilterDto.getReferenceType()) &&
                                Objects.equals(orderPaymentFilterForOp.getValue(), paymentFilterDto.getValue())
                ).collect(Collectors.toList()).get(0);

        PaymentFilterDto thirdExpectedFilterDto = Objects.requireNonNull(response.getBody())
                .stream()
                .filter(paymentFilterDto ->
                        Objects.equals(merchantPaymentFilterForNordea.getReferenceType(), paymentFilterDto.getReferenceType()) &&
                                Objects.equals(merchantPaymentFilterForNordea.getValue(), paymentFilterDto.getValue())
                ).collect(Collectors.toList()).get(0);

        PaymentFilterDto fourthExpectedFilterDto = Objects.requireNonNull(response.getBody())
                .stream()
                .filter(paymentFilterDto ->
                        Objects.equals(merchantPaymentFilterForOp.getReferenceType(), paymentFilterDto.getReferenceType()) &&
                                Objects.equals(merchantPaymentFilterForOp.getValue(), paymentFilterDto.getValue())
                ).collect(Collectors.toList()).get(0);


        Assertions.assertNotNull(firstExpectedFilterDto.getFilterId());
        Assertions.assertEquals(firstExpectedFilterDto.getFilterId(), getActualFilterId(orderPaymentFilterForNordea));
        Assertions.assertEquals(firstExpectedFilterDto.getReferenceId(), orderPaymentFilterForNordea.getReferenceId());
        Assertions.assertEquals(firstExpectedFilterDto.getReferenceType(), orderPaymentFilterForNordea.getReferenceType());
        Assertions.assertEquals(firstExpectedFilterDto.getFilterType(), orderPaymentFilterForNordea.getFilterType());
        Assertions.assertEquals(firstExpectedFilterDto.getValue(), orderPaymentFilterForNordea.getValue());
        Assertions.assertNotNull(firstExpectedFilterDto.getCreatedAt());

        Assertions.assertNotNull(secondExpectedFilterDto.getFilterId());
        Assertions.assertEquals(secondExpectedFilterDto.getFilterId(), getActualFilterId(orderPaymentFilterForOp));
        Assertions.assertEquals(secondExpectedFilterDto.getReferenceId(), orderPaymentFilterForOp.getReferenceId());
        Assertions.assertEquals(secondExpectedFilterDto.getReferenceType(), orderPaymentFilterForOp.getReferenceType());
        Assertions.assertEquals(secondExpectedFilterDto.getFilterType(), orderPaymentFilterForOp.getFilterType());
        Assertions.assertEquals(secondExpectedFilterDto.getValue(), orderPaymentFilterForOp.getValue());
        Assertions.assertNotNull(secondExpectedFilterDto.getCreatedAt());

        Assertions.assertNotNull(thirdExpectedFilterDto.getFilterId());
        Assertions.assertEquals(thirdExpectedFilterDto.getFilterId(), getActualFilterId(merchantPaymentFilterForNordea));
        Assertions.assertEquals(thirdExpectedFilterDto.getReferenceId(), merchantPaymentFilterForNordea.getReferenceId());
        Assertions.assertEquals(thirdExpectedFilterDto.getReferenceType(), merchantPaymentFilterForNordea.getReferenceType());
        Assertions.assertEquals(thirdExpectedFilterDto.getFilterType(), merchantPaymentFilterForNordea.getFilterType());
        Assertions.assertEquals(thirdExpectedFilterDto.getValue(), merchantPaymentFilterForNordea.getValue());
        Assertions.assertNotNull(thirdExpectedFilterDto.getCreatedAt());

        Assertions.assertNotNull(fourthExpectedFilterDto.getFilterId());
        Assertions.assertEquals(fourthExpectedFilterDto.getFilterId(), getActualFilterId(merchantPaymentFilterForOp));
        Assertions.assertEquals(fourthExpectedFilterDto.getReferenceId(), merchantPaymentFilterForOp.getReferenceId());
        Assertions.assertEquals(fourthExpectedFilterDto.getReferenceType(), merchantPaymentFilterForOp.getReferenceType());
        Assertions.assertEquals(fourthExpectedFilterDto.getFilterType(), merchantPaymentFilterForOp.getFilterType());
        Assertions.assertEquals(fourthExpectedFilterDto.getValue(), merchantPaymentFilterForOp.getValue());
        Assertions.assertNotNull(fourthExpectedFilterDto.getCreatedAt());

        // remove test filters from database
        paymentFiltersToBeDeleted.add(getActualFilterId(orderPaymentFilterForNordea));
        paymentFiltersToBeDeleted.add(getActualFilterId(orderPaymentFilterForOp));
        paymentFiltersToBeDeleted.add(getActualFilterId(merchantPaymentFilterForNordea));
        paymentFiltersToBeDeleted.add(getActualFilterId(merchantPaymentFilterForOp));
    }

    /*
     * It tests the create payment method endpoint.
     */
    @Test
    @RunIfProfile(profile = "local")
    public void whenCreatePaymentMethodWithValidDataThenReturnStatus201() {
        PaymentMethodDto paymentMethodDto = createTestPaymentMethodDto(GatewayEnum.ONLINE);
        ResponseEntity<PaymentMethodDto> response = paymentAdminController.createPaymentMethod(paymentMethodDto);

        PaymentMethodDto responsePaymentMethodDto = response.getBody();
        Assertions.assertNotNull(responsePaymentMethodDto);
        String code = responsePaymentMethodDto.getCode();
        paymentMethodsToBeDeleted.add(code);
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
    public void whenCreatePaymentMethodWithSameCodeThatExistsThenReturnError409() {
        createTestPaymentMethod();
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
    public void whenUpdatePaymentMethodWithValidDataThenReturnStatus200() {
        createTestPaymentMethod();

        String code = "test-payment-code";
        PaymentMethodDto updatedPaymentMethodDto = createTestPaymentMethodDto(GatewayEnum.OFFLINE);
        updatedPaymentMethodDto.setName("Edited test payment method");

        ResponseEntity<PaymentMethodDto> response = paymentAdminController.updatePaymentMethod(code, updatedPaymentMethodDto);

        PaymentMethodDto responsePaymentMethodDto = response.getBody();
        Assertions.assertNotNull(responsePaymentMethodDto);
        String resCode = responsePaymentMethodDto.getCode();
        paymentMethodsToBeDeleted.add(resCode);
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
    public void whenUpdatePaymentMethodThatDoesNotExistThenReturnError404() {
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
    public void whenDeletePaymentMethodThatExistsThenReturnStatus200() {
        createTestPaymentMethod();

        String code = "test-payment-code";
        ResponseEntity<String> response = paymentAdminController.deletePaymentMethod(code);
        String responseCode = response.getBody();
        Assertions.assertNotNull(responseCode);
        assertEquals(code, responseCode);

    }

    @Test
    @RunIfProfile(profile = "local")
    public void whenDeletePaymentMethodThatDoesNotExistThenReturnError404() {
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
    public void whenGetPaymentMethodByCodeThatDoesNotExistThenReturnStatus200() {
        createTestPaymentMethod();

        String code = "test-payment-code";
        ResponseEntity<PaymentMethodDto> response = paymentAdminController.getPaymentMethodByCode(code);

        PaymentMethodDto responsePaymentMethodDto = response.getBody();
        Assertions.assertNotNull(responsePaymentMethodDto);
        String resCode = responsePaymentMethodDto.getCode();
        Assertions.assertNotNull(resCode);
        paymentMethodsToBeDeleted.add(resCode);
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
    public void whenGetPaymentMethodByCodeThatDoesNotExistThenReturnError404() {
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
    public void whenGetPaymentMethodsThenReturnMoreThanZeroWithStatus200() {
        createTestPaymentMethod();

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
        paymentMethodsToBeDeleted.add(resCode);
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

    private void createTestPaymentMethod() {
        PaymentMethodModel paymentMethodModel = new PaymentMethodModel();
        paymentMethodModel.setName("Test payment method");
        paymentMethodModel.setCode("test-payment-code");
        paymentMethodModel.setGroup("test-payment-group");
        paymentMethodModel.setImg("test-payment.jpg");
        paymentMethodModel.setGateway(GatewayEnum.ONLINE);
        PaymentMethodModel saved = paymentMethodRepository.save(paymentMethodModel);
        paymentMethodsToBeDeleted.add(saved.getCode());
    }

    public void paymentFilterDuplicatePrevention() throws JsonProcessingException {
        List<PaymentFilterDto> request = new ArrayList<>();
        PaymentFilterDto orderPaymentFilterForNordea = new PaymentFilterDto();
        orderPaymentFilterForNordea.setNamespace("testi2-namespace");
        orderPaymentFilterForNordea.setReferenceId("order-2");
        orderPaymentFilterForNordea.setReferenceType("order");
        orderPaymentFilterForNordea.setFilterType("banks");
        orderPaymentFilterForNordea.setValue("nordea");

        PaymentFilterDto orderPaymentFilterForNordeaDuplicate = new PaymentFilterDto();
        orderPaymentFilterForNordeaDuplicate.setNamespace("testi2-namespace");
        orderPaymentFilterForNordeaDuplicate.setReferenceId("order-2");
        orderPaymentFilterForNordeaDuplicate.setReferenceType("order");
        orderPaymentFilterForNordeaDuplicate.setFilterType("banks");
        orderPaymentFilterForNordeaDuplicate.setValue("nordea");

        request.add(orderPaymentFilterForNordea);
        request.add(orderPaymentFilterForNordeaDuplicate);

        ResponseEntity<List<PaymentFilterDto>> response = paymentAdminController.savePaymentFilters(request);
        log.info("response : {}",mapper.writeValueAsString(response.getBody()));
        Assertions.assertEquals(response.getStatusCode(), HttpStatus.CREATED);
        Assertions.assertEquals(1, Objects.requireNonNull(response.getBody()).size());

        paymentFiltersToBeDeleted.add(response.getBody().get(0).getFilterId());

        List<PaymentFilter> foundFilters = (List<PaymentFilter>) paymentFilterRepository.findAllById(paymentFiltersToBeDeleted);
        PaymentFilter paymentFilter = foundFilters.get(0);
        Assertions.assertNotNull(paymentFilter.getFilterId());
        Assertions.assertNotNull(paymentFilter.getCreatedAt());
        Assertions.assertEquals(paymentFilter.getFilterId(), getActualFilterId(orderPaymentFilterForNordea));
        Assertions.assertEquals(orderPaymentFilterForNordea.getValue(), paymentFilter.getValue());
        Assertions.assertEquals(orderPaymentFilterForNordea.getFilterType(), paymentFilter.getFilterType());
        Assertions.assertEquals(orderPaymentFilterForNordea.getReferenceType(), paymentFilter.getReferenceType());
        Assertions.assertEquals(orderPaymentFilterForNordea.getReferenceId(), paymentFilter.getReferenceId());
        Assertions.assertEquals(orderPaymentFilterForNordea.getNamespace(), paymentFilter.getNamespace());
    }

    public String getActualFilterId(PaymentFilterDto filterDto){
        String valueReferenceIdUUID3 = UUIDGenerator.generateType3UUIDString(filterDto.getNamespace(), filterDto.getReferenceId());
        String valueReferenceIdValueUUID3 = UUIDGenerator.generateType3UUIDString(valueReferenceIdUUID3, filterDto.getValue());
        String valueReferenceIdValueReferenceType = UUIDGenerator.generateType3UUIDString(valueReferenceIdValueUUID3, filterDto.getReferenceType());
        return valueReferenceIdValueReferenceType;
    }
}

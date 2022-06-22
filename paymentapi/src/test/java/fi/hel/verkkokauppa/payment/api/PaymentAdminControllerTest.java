package fi.hel.verkkokauppa.payment.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.payment.api.data.PaymentFilterDto;
import fi.hel.verkkokauppa.payment.model.PaymentFilter;
import fi.hel.verkkokauppa.payment.repository.PaymentFilterRepository;
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

@SpringBootTest
@Slf4j
public class PaymentAdminControllerTest {
    List<String> idsToDelete = new ArrayList<>();
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private PaymentAdminController paymentAdminController;

    @Autowired
    private PaymentFilterRepository paymentFilterRepository;

    @AfterEach
    void tearDown() {
        try {
            idsToDelete.forEach(s -> paymentFilterRepository.deleteById(s));
            // Clear list because all filters deleted
            idsToDelete = new ArrayList<>();
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
        idsToDelete.add(getActualFilterId(orderPaymentFilterForNordea));
        idsToDelete.add(getActualFilterId(orderPaymentFilterForOp));
        idsToDelete.add(getActualFilterId(merchantPaymentFilterForNordea));
        idsToDelete.add(getActualFilterId(merchantPaymentFilterForOp));
    }

    @Test
    @RunIfProfile(profile = "local")
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
        Assertions.assertEquals(1,Objects.requireNonNull(response.getBody()).size());

        idsToDelete.add(response.getBody().get(0).getFilterId());

        List<PaymentFilter> foundFilters = (List<PaymentFilter>) paymentFilterRepository.findAllById(idsToDelete);
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
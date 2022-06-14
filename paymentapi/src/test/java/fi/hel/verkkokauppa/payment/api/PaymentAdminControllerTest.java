package fi.hel.verkkokauppa.payment.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.payment.api.data.PaymentFilterDto;
import fi.hel.verkkokauppa.payment.model.PaymentFilter;
import fi.hel.verkkokauppa.payment.repository.PaymentFilterRepository;
import fi.hel.verkkokauppa.payment.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
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

    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private PaymentAdminController paymentAdminController;

    @Autowired
    private PaymentFilterRepository paymentFilterRepository;

    @Test
    @RunIfProfile(profile = "local")
    public void savePaymentFilter() throws JsonProcessingException {
        List<PaymentFilterDto> request = new ArrayList<>();
        PaymentFilterDto orderPaymentFilterForNordea = new PaymentFilterDto();
        orderPaymentFilterForNordea.setReferenceId("order-1");
        orderPaymentFilterForNordea.setReferenceType("order");
        orderPaymentFilterForNordea.setNamespace("testi-namespace");
        orderPaymentFilterForNordea.setType("banks");
        orderPaymentFilterForNordea.setValue("nordea");

        PaymentFilterDto orderPaymentFilterForOp = new PaymentFilterDto();
        orderPaymentFilterForOp.setReferenceId("order-1");
        orderPaymentFilterForOp.setReferenceType("order");
        orderPaymentFilterForOp.setNamespace("testi-namespace");
        orderPaymentFilterForOp.setType("banks");
        orderPaymentFilterForOp.setValue("op");

        PaymentFilterDto merchantPaymentFilterForNordea = new PaymentFilterDto();
        merchantPaymentFilterForNordea.setReferenceId("merchant-1");
        merchantPaymentFilterForNordea.setReferenceType("merchant");
        merchantPaymentFilterForNordea.setNamespace("testi-namespace");
        merchantPaymentFilterForNordea.setType("banks");
        merchantPaymentFilterForNordea.setValue("nordea");

        PaymentFilterDto merchantPaymentFilterForOp = new PaymentFilterDto();
        merchantPaymentFilterForOp.setReferenceId("merchant-1");
        merchantPaymentFilterForOp.setReferenceType("merchant");
        merchantPaymentFilterForOp.setNamespace("testi-namespace");
        merchantPaymentFilterForOp.setType("banks");
        merchantPaymentFilterForOp.setValue("op");

        // remove from database before querying
        List<String> idsToDelete = new ArrayList<>();
        idsToDelete.add(getActualFilterId(orderPaymentFilterForNordea));
        idsToDelete.add(getActualFilterId(orderPaymentFilterForOp));
        idsToDelete.add(getActualFilterId(merchantPaymentFilterForNordea));
        idsToDelete.add(getActualFilterId(merchantPaymentFilterForOp));
        try {
            paymentFilterRepository.deleteAllById(idsToDelete);
        } catch (Exception e) {
            // do nothing if delete fails
        }

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
        Assertions.assertEquals(firstExpectedFilterDto.getType(), orderPaymentFilterForNordea.getType());
        Assertions.assertEquals(firstExpectedFilterDto.getValue(), orderPaymentFilterForNordea.getValue());
        Assertions.assertNotNull(firstExpectedFilterDto.getCreatedAt());

        Assertions.assertNotNull(secondExpectedFilterDto.getFilterId());
        Assertions.assertEquals(secondExpectedFilterDto.getFilterId(), getActualFilterId(orderPaymentFilterForOp));
        Assertions.assertEquals(secondExpectedFilterDto.getReferenceId(), orderPaymentFilterForOp.getReferenceId());
        Assertions.assertEquals(secondExpectedFilterDto.getReferenceType(), orderPaymentFilterForOp.getReferenceType());
        Assertions.assertEquals(secondExpectedFilterDto.getType(), orderPaymentFilterForOp.getType());
        Assertions.assertEquals(secondExpectedFilterDto.getValue(), orderPaymentFilterForOp.getValue());
        Assertions.assertNotNull(secondExpectedFilterDto.getCreatedAt());

        Assertions.assertNotNull(thirdExpectedFilterDto.getFilterId());
        Assertions.assertEquals(thirdExpectedFilterDto.getFilterId(), getActualFilterId(merchantPaymentFilterForNordea));
        Assertions.assertEquals(thirdExpectedFilterDto.getReferenceId(), merchantPaymentFilterForNordea.getReferenceId());
        Assertions.assertEquals(thirdExpectedFilterDto.getReferenceType(), merchantPaymentFilterForNordea.getReferenceType());
        Assertions.assertEquals(thirdExpectedFilterDto.getType(), merchantPaymentFilterForNordea.getType());
        Assertions.assertEquals(thirdExpectedFilterDto.getValue(), merchantPaymentFilterForNordea.getValue());
        Assertions.assertNotNull(thirdExpectedFilterDto.getCreatedAt());

        Assertions.assertNotNull(fourthExpectedFilterDto.getFilterId());
        Assertions.assertEquals(fourthExpectedFilterDto.getFilterId(), getActualFilterId(merchantPaymentFilterForOp));
        Assertions.assertEquals(fourthExpectedFilterDto.getReferenceId(), merchantPaymentFilterForOp.getReferenceId());
        Assertions.assertEquals(fourthExpectedFilterDto.getReferenceType(), merchantPaymentFilterForOp.getReferenceType());
        Assertions.assertEquals(fourthExpectedFilterDto.getType(), merchantPaymentFilterForOp.getType());
        Assertions.assertEquals(fourthExpectedFilterDto.getValue(), merchantPaymentFilterForOp.getValue());
        Assertions.assertNotNull(fourthExpectedFilterDto.getCreatedAt());

    }

    @Test
    @RunIfProfile(profile = "local")
    public void paymentFilterDuplicatePrevention() throws JsonProcessingException {
        List<PaymentFilterDto> request = new ArrayList<>();
        PaymentFilterDto orderPaymentFilterForNordea = new PaymentFilterDto();
        orderPaymentFilterForNordea.setNamespace("testi-namespace");
        orderPaymentFilterForNordea.setReferenceId("order-1");
        orderPaymentFilterForNordea.setReferenceType("order");
        orderPaymentFilterForNordea.setType("banks");
        orderPaymentFilterForNordea.setValue("nordea");

        PaymentFilterDto orderPaymentFilterForNordeaDuplicate = new PaymentFilterDto();
        orderPaymentFilterForNordeaDuplicate.setNamespace("testi-namespace");
        orderPaymentFilterForNordeaDuplicate.setReferenceId("order-1");
        orderPaymentFilterForNordeaDuplicate.setReferenceType("order");
        orderPaymentFilterForNordeaDuplicate.setType("banks");
        orderPaymentFilterForNordeaDuplicate.setValue("nordea");

        // remove from database before querying
        List<String> idsToDelete = new ArrayList<>();
        idsToDelete.add(getActualFilterId(orderPaymentFilterForNordea));
        idsToDelete.add(getActualFilterId(orderPaymentFilterForNordeaDuplicate));
        try {
            paymentFilterRepository.deleteAllById(idsToDelete);
        } catch (Exception e) {
            // do nothing if delete fails
        }

        request.add(orderPaymentFilterForNordea);
        request.add(orderPaymentFilterForNordeaDuplicate);

        ResponseEntity<List<PaymentFilterDto>> response = paymentAdminController.savePaymentFilters(request);
        log.info("response : {}",mapper.writeValueAsString(response.getBody()));
        Assertions.assertEquals(response.getStatusCode(), HttpStatus.CREATED);
        Assertions.assertEquals(Objects.requireNonNull(response.getBody()).size(),2);

        List<PaymentFilter> foundFilters = paymentFilterRepository.findByNamespaceAndReferenceIdAndReferenceTypeAndValue(
                "testi-namespace", "order-1", "order", "nordea"
        );
        Assertions.assertEquals(foundFilters.size(), 1);
    }

    public String getActualFilterId(PaymentFilterDto filterDto){
        String valueReferenceIdUUID3 = UUIDGenerator.generateType3UUIDString(filterDto.getNamespace(), filterDto.getReferenceId());
        String valueReferenceIdValueUUID3 = UUIDGenerator.generateType3UUIDString(valueReferenceIdUUID3, filterDto.getValue());
        String valueReferenceIdValueReferenceType = UUIDGenerator.generateType3UUIDString(valueReferenceIdValueUUID3, filterDto.getReferenceType());
        return valueReferenceIdValueReferenceType;
    }
}
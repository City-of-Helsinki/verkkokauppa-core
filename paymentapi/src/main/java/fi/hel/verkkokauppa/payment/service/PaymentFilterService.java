package fi.hel.verkkokauppa.payment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.payment.api.data.PaymentFilterDto;
import fi.hel.verkkokauppa.payment.model.PaymentFilter;
import fi.hel.verkkokauppa.payment.repository.PaymentFilterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class PaymentFilterService {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PaymentFilterRepository paymentFilterRepository;

    public List<PaymentFilter> savePaymentFilters(List<PaymentFilterDto> paymentFiltersDto) {

        if (paymentFiltersDto.isEmpty()) {
            throw new CommonApiException(
                    HttpStatus.BAD_REQUEST,
                    new Error("empty-payment-filter-list", "no payment filters given to save")
            );
        }

        List<PaymentFilter> paymentFilters = new ArrayList<>();

        for (PaymentFilterDto paymentFilterDto : paymentFiltersDto) {
            PaymentFilter paymentFilterToBeSaved;
            paymentFilterToBeSaved = objectMapper.convertValue(paymentFilterDto, PaymentFilter.class);
            paymentFilterToBeSaved.setUUID3FilterIdFromNamespaceAndValueAndReferenceIdAndReferenceType();
            paymentFilterToBeSaved.setCreatedAt(DateTimeUtil.getFormattedDateTime());

            paymentFilters.add(paymentFilterToBeSaved);
        }


        List<PaymentFilter> savedFilters = (List<PaymentFilter>) paymentFilterRepository.saveAll(paymentFilters);
        return new ArrayList<>(savedFilters.stream()
                .collect(Collectors.toCollection(
                        () -> new TreeSet<>(Comparator.comparing(PaymentFilter::getFilterId))
                )));
    }

    public List<PaymentFilter> findPaymentFiltersByReferenceTypeAndReferenceId(String referenceType, String referenceId) {
        return paymentFilterRepository.findByReferenceTypeAndReferenceId(referenceType, referenceId);
    }

    public List<PaymentFilterDto> mapPaymentFilterListToDtoList(List<PaymentFilter> paymentFilters) {
        return objectMapper.convertValue(paymentFilters, new TypeReference<List<PaymentFilterDto>>(){});
    }
}

package fi.hel.verkkokauppa.payment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.payment.api.data.PaymentFilterDto;
import fi.hel.verkkokauppa.payment.model.PaymentFilter;
import fi.hel.verkkokauppa.payment.repository.PaymentFilterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PaymentFilterService {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PaymentFilterRepository paymentFilterRepository;

    public List<PaymentFilter> savePaymentFilters(List<PaymentFilterDto> paymentFiltersDto) {

        List<PaymentFilter> paymentFilters = new ArrayList<>();

        for (PaymentFilterDto paymentFilterDto : paymentFiltersDto) {
            PaymentFilter paymentFilterToBeSaved;
            paymentFilterToBeSaved = objectMapper.convertValue(paymentFilterDto, PaymentFilter.class);
            paymentFilterToBeSaved.setUUID3FilterIdFromNamespaceAndValueAndReferenceIdAndReferenceType();
            paymentFilterToBeSaved.setCreatedAt(DateTimeUtil.getFormattedDateTime());

            paymentFilters.add(paymentFilterToBeSaved);
        }

        return (List<PaymentFilter>) paymentFilterRepository.saveAll(paymentFilters);
    }

    public List<PaymentFilterDto> mapPaymentFilterListToDtoList(List<PaymentFilter> paymentFilters) {
        return objectMapper.convertValue(paymentFilters, new TypeReference<List<PaymentFilterDto>>(){});
    }
}

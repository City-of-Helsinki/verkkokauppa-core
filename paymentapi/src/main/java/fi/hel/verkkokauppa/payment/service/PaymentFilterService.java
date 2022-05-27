package fi.hel.verkkokauppa.payment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.payment.api.data.PaymentFilterDto;
import fi.hel.verkkokauppa.payment.model.PaymentFilter;
import fi.hel.verkkokauppa.payment.repository.PaymentFilterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PaymentFilterService {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PaymentFilterRepository paymentFilterRepository;

    public List<PaymentFilter> savePaymentFilters(List<PaymentFilterDto> paymentFiltersDto) {

        List<PaymentFilter> paymentFilters = new ArrayList<PaymentFilter>();

        for (PaymentFilterDto paymentFilterDto : paymentFiltersDto) {
            PaymentFilter paymentFilter = new PaymentFilter();
            //paymentFilter.setFilterId(UUIDGenerator.generateType3UUIDString(paymentFilterDto.getReferenceId(), paymentFilterDto.getType()));
            //paymentFilter.setCreatedAt(LocalDateTime.now());
            //paymentFilter.setUpdatedAt(LocalDateTime.now());
            paymentFilter.setReferenceId(paymentFilterDto.getReferenceId());
            paymentFilter.setType(paymentFilterDto.getType());
            paymentFilter.setValue(paymentFilterDto.getValue());
            
            paymentFilters.add(paymentFilter);
        }

        return (List<PaymentFilter>) paymentFilterRepository.saveAll(paymentFilters);
    }

    public List<PaymentFilterDto> mapToDto(List<PaymentFilter> paymentFilters) {
        return objectMapper.convertValue(paymentFilters, new TypeReference<List<PaymentFilterDto>>(){});
    }
}

package fi.hel.verkkokauppa.payment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.payment.api.data.PaymentFilterDto;
import fi.hel.verkkokauppa.payment.model.PaymentFilter;
import fi.hel.verkkokauppa.payment.repository.PaymentFilterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class PaymentFilterService {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PaymentFilterRepository paymentFilterRepository;

    public List<PaymentFilter> savePaymentFilters(List<PaymentFilterDto> paymentFiltersDto) {

        List<PaymentFilter> updatedFilters = new ArrayList<>();
        List<PaymentFilter> createdFilters = new ArrayList<>();

        for (PaymentFilterDto paymentFilterDto : paymentFiltersDto) {

            String dtoFilterId = paymentFilterDto.getFilterId();
            if (dtoFilterId != null) {
                // Update filter
                PaymentFilter paymentFilter = paymentFilterRepository.findByFilterId(dtoFilterId);

                paymentFilter.setNamespace(paymentFilterDto.getNamespace());
                paymentFilter.setValue(paymentFilterDto.getValue());
                paymentFilter.setType(paymentFilterDto.getType());
                paymentFilter.setReferenceType(paymentFilterDto.getReferenceType());
                paymentFilter.setUpdatedAt(DateTimeUtil.getFormattedDateTime());

                // set new filterId before saving from value refId and refType
                paymentFilter.setUUID3FilterIdFromValueAndReferenceIdAndReferenceType();
                // Save and add to updatedFilters list
                PaymentFilter updatedFilter = paymentFilterRepository.save(paymentFilter);
                // Add to updatedFilters list
                updatedFilters.add(updatedFilter);
                // Remove old filter because update changes filter id
                paymentFilterRepository.deleteById(dtoFilterId);
            }
            else {
                // Create new filter
                // Käytetään objectMapperiä to ja mäpätään dto PaymentFilteriin, siirretty funktioon jos halutaan joskus jättää jotain pois mäppäyksestä
                PaymentFilter paymentFilter = mapFromDto(paymentFilterDto);
                // Asetetaan id valuesta ja reference id:stä
                paymentFilter.setUUID3FilterIdFromValueAndReferenceIdAndReferenceType();
                // Asetetaan created at nykyhetkeen
                paymentFilter.setCreatedAt(DateTimeUtil.getFormattedDateTime());

                // Asetetaan luotu payment filter cretedFilters taulukkoon
                createdFilters.add(paymentFilterRepository.save(paymentFilter));
            }

        }
        // Create empty list to return both updated and created filters
        List<PaymentFilter> allFilters = new ArrayList<>();
        // Add all updated filters to allFilters list
        allFilters.addAll(updatedFilters);
        // Add all created filters to allFilters list
        allFilters.addAll(createdFilters);

        // allFilters contains updated and created filters. Filters out duplicates using filterId
        return new ArrayList<>(allFilters.stream()
                .collect(Collectors.toCollection(
                        () -> new TreeSet<>(Comparator.comparing(PaymentFilter::getFilterId))
                )));
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> keyExtractor.apply(t)!=null && seen.add(keyExtractor.apply(t));
    }

    private PaymentFilter mapFromDto(PaymentFilterDto paymentFilterDto) {
        return objectMapper.convertValue(paymentFilterDto, PaymentFilter.class);
    }

    public List<PaymentFilterDto> mapPaymentFilterListToDtoList(List<PaymentFilter> paymentFilters) {
        return objectMapper.convertValue(paymentFilters, new TypeReference<List<PaymentFilterDto>>() {
        });
    }
}

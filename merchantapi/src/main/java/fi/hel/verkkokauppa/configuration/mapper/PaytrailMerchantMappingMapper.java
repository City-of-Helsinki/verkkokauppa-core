package fi.hel.verkkokauppa.configuration.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.configuration.api.merchant.dto.PaytrailMerchantMappingDto;
import fi.hel.verkkokauppa.configuration.model.merchant.PaytrailMerchantMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PaytrailMerchantMappingMapper {
    @Autowired
    ObjectMapper objectMapper;

    public PaytrailMerchantMapping fromDto(PaytrailMerchantMappingDto dto) { return objectMapper.convertValue(dto, PaytrailMerchantMapping.class); }

    public PaytrailMerchantMappingDto toDto(PaytrailMerchantMapping entity) { return objectMapper.convertValue(entity, PaytrailMerchantMappingDto.class); }
}

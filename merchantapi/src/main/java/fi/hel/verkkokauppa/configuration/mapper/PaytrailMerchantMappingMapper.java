package fi.hel.verkkokauppa.configuration.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.mapper.AbstractModelMapper;
import fi.hel.verkkokauppa.configuration.api.merchant.dto.PaytrailMerchantMappingDto;
import fi.hel.verkkokauppa.configuration.model.merchant.PaytrailMerchantMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PaytrailMerchantMappingMapper extends AbstractModelMapper<PaytrailMerchantMapping, PaytrailMerchantMappingDto> {
    @Autowired
    public PaytrailMerchantMappingMapper(ObjectMapper mapper) { super(mapper, PaytrailMerchantMapping::new, PaytrailMerchantMappingDto::new); }
}

package fi.hel.verkkokauppa.configuration.api.namespace.dto;

import fi.hel.verkkokauppa.configuration.api.merchant.dto.MerchantDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NamespaceAndMerchantWrapperDto {
    NamespaceDto namespace;
    List<MerchantDto> merchants;
}

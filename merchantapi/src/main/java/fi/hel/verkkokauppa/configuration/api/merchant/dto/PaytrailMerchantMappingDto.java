package fi.hel.verkkokauppa.configuration.api.merchant.dto;

import lombok.Data;
import lombok.Getter;

import javax.validation.constraints.NotNull;

@Data
@Getter
public class PaytrailMerchantMappingDto {
    @NotNull
    private String namespace;
    @NotNull
    private String merchantPaytrailMerchantId;
    @NotNull
    private String merchantPaytrailSecret;
    private String description;
    private String id;
}

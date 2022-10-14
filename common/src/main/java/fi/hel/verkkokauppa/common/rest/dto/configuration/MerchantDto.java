package fi.hel.verkkokauppa.common.rest.dto.configuration;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Data
public class MerchantDto {
    String merchantId;
    String namespace;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    ArrayList<ConfigurationDto> configurations;
}

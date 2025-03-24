package fi.hel.verkkokauppa.message.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Getter
@Setter
public class MerchantDto {
    String merchantId;
    String namespace;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    ArrayList<ConfigurationModelDto> configurations;
    String merchantPaytrailMerchantId;
}


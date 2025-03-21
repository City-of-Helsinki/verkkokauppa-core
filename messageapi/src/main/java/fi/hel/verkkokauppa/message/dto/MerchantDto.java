package fi.hel.verkkokauppa.message.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class MerchantDto {
    String merchantId;
    String namespace;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    ArrayList<ConfigurationModelDto> configurations;
    String merchantPaytrailMerchantId;
}


package fi.hel.verkkokauppa.configuration.api.merchant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fi.hel.verkkokauppa.configuration.model.ConfigurationModel;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MerchantDto {
    // If given do not create new merchant
    String merchantId;
    @NotBlank(message = "namespace cant be blank")
    String namespace;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    ArrayList<ConfigurationModel> configurations;
    String merchantPaytrailMerchantId;
}


package fi.hel.verkkokauppa.merchant.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fi.hel.verkkokauppa.merchant.model.ConfigurationModel;
import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MerchantDto {
    // If given do not create new merchant
    String merchantId;
    @NotBlank(message = "namespace cant be blank")
    String namespace;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    ArrayList<ConfigurationModel> configurations;
}


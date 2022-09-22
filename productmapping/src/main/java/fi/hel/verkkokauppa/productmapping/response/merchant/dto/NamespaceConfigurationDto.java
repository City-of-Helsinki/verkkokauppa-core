package fi.hel.verkkokauppa.productmapping.response.merchant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import fi.hel.verkkokauppa.productmapping.response.merchant.ConfigurationModel;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NamespaceConfigurationDto {
   ConfigurationModel configuration;
}


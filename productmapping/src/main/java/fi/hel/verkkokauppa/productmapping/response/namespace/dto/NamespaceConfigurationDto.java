package fi.hel.verkkokauppa.productmapping.response.namespace.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import fi.hel.verkkokauppa.productmapping.response.namespace.ConfigurationModel;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NamespaceConfigurationDto {
   ConfigurationModel configuration;
}


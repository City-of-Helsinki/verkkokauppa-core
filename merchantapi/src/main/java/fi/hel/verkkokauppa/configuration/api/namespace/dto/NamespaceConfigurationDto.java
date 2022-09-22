package fi.hel.verkkokauppa.configuration.api.namespace.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fi.hel.verkkokauppa.configuration.model.ConfigurationModel;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NamespaceConfigurationDto {
   ConfigurationModel configuration;
}


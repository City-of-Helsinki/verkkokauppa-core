package fi.hel.verkkokauppa.common.rest.dto.configuration;

import lombok.Data;

@Data
public class ServiceConfigurationDto {

    private String configurationId;
    private String namespace;
    private String configurationKey;
    private String configurationValue;
    private boolean restricted;
}

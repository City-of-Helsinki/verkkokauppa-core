package fi.hel.verkkokauppa.common.rest.dto.configuration;

import lombok.Data;

@Data
public class ConfigurationDto {
    private String key;
    private String value;
    private boolean restricted;
    private LocaleDto locale;
}

package fi.hel.verkkokauppa.configuration.api.merchant.dto;

import lombok.Data;

@Data
public class ConfigurationDto {
    private String key;
    private String value;
    private boolean restricted;
    private LocaleDto locale;
}

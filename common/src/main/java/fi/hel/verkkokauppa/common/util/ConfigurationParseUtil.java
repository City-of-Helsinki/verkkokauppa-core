package fi.hel.verkkokauppa.common.util;

import fi.hel.verkkokauppa.common.rest.dto.configuration.ConfigurationDto;
import fi.hel.verkkokauppa.common.rest.dto.configuration.MerchantDto;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ConfigurationParseUtil {

    public static Optional<ConfigurationDto> parseConfigurationValueByKey(List<ConfigurationDto> configurations, String key) {
        return configurations
                .stream()
                .filter(configurationModel -> Objects.equals(configurationModel.getKey(), key))
                .findFirst();
    }

    public static MerchantDto modifyMerchantConfigurationValueByKey(MerchantDto merchantDto, String key, String value) {
        MerchantDto merchantToUpdate = merchantDto;
        merchantToUpdate.getConfigurations().stream()
                .filter(configurationModel -> Objects.equals(configurationModel.getKey(), key))
                .forEach(configurationModel -> configurationModel.setValue(value));
        return merchantToUpdate;

    }
}

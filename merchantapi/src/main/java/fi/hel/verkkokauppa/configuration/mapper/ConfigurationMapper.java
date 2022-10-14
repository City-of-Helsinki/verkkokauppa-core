package fi.hel.verkkokauppa.configuration.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.mapper.AbstractModelMapper;
import fi.hel.verkkokauppa.configuration.api.merchant.dto.ConfigurationDto;
import fi.hel.verkkokauppa.configuration.model.ConfigurationModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationMapper extends AbstractModelMapper<ConfigurationModel, ConfigurationDto> {

        @Autowired
        public ConfigurationMapper(ObjectMapper mapper) {
            super(mapper, ConfigurationModel::new, ConfigurationDto::new);
        }
}

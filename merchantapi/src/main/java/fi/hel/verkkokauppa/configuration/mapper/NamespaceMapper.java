package fi.hel.verkkokauppa.configuration.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.mapper.AbstractModelMapper;
import fi.hel.verkkokauppa.configuration.api.namespace.dto.NamespaceDto;
import fi.hel.verkkokauppa.configuration.model.namespace.NamespaceModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * > This class is a Spring component that maps a NamespaceModel object to a NamespaceDto object and back
 */
@Component
public class NamespaceMapper extends AbstractModelMapper<NamespaceModel, NamespaceDto> {

    @Autowired
    public NamespaceMapper(ObjectMapper mapper) {
        super(mapper, NamespaceModel::new, NamespaceDto::new);
    }
}

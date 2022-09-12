package fi.hel.verkkokauppa.productmapping.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.mapper.AbstractModelMapper;
import fi.hel.verkkokauppa.productmapping.api.data.ProductMappingDto;
import fi.hel.verkkokauppa.productmapping.model.product.ProductMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProductMappingMapper extends AbstractModelMapper <ProductMapping, ProductMappingDto> {

    @Autowired
    public ProductMappingMapper(ObjectMapper mapper) {
        super(mapper, ProductMapping::new, ProductMappingDto::new);
    }
}

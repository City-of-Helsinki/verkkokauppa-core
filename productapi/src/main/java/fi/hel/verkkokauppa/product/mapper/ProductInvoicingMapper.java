package fi.hel.verkkokauppa.product.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.mapper.AbstractModelMapper;
import fi.hel.verkkokauppa.product.dto.ProductInvoicingDto;
import fi.hel.verkkokauppa.product.model.ProductInvoicing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProductInvoicingMapper extends AbstractModelMapper<ProductInvoicing, ProductInvoicingDto> {
    @Autowired
    public ProductInvoicingMapper(ObjectMapper mapper) {
        super(mapper, ProductInvoicing::new, ProductInvoicingDto::new);
    }
}

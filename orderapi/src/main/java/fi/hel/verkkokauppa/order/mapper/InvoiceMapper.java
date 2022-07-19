package fi.hel.verkkokauppa.order.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.order.api.data.invoice.InvoiceDto;
import fi.hel.verkkokauppa.order.model.invoice.Invoice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * `InvoiceMapper` is a `@Component` that extends `AbstractModelMapper` and uses the `ObjectMapper` to map between
 * `Invoice` and `InvoiceDto` objects
 */
@Component
public class InvoiceMapper extends AbstractModelMapper<Invoice, InvoiceDto> {

    @Autowired
    public InvoiceMapper(ObjectMapper mapper) {
        super(mapper, Invoice::new, InvoiceDto::new);
    }
}

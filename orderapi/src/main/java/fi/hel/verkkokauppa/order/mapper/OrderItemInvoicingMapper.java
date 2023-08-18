package fi.hel.verkkokauppa.order.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.mapper.AbstractModelMapper;
import fi.hel.verkkokauppa.order.api.data.invoice.OrderItemInvoicingDto;
import fi.hel.verkkokauppa.order.model.invoice.OrderItemInvoicing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderItemInvoicingMapper extends AbstractModelMapper<OrderItemInvoicing, OrderItemInvoicingDto> {
    @Autowired
    public OrderItemInvoicingMapper(ObjectMapper mapper) {
        super(mapper, OrderItemInvoicing::new, OrderItemInvoicingDto::new);
    }
}

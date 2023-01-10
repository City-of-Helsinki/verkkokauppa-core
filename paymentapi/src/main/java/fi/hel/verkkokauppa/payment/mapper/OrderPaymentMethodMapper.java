package fi.hel.verkkokauppa.payment.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.mapper.AbstractModelMapper;
import fi.hel.verkkokauppa.payment.api.data.OrderPaymentMethodDto;
import fi.hel.verkkokauppa.payment.model.OrderPaymentMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderPaymentMethodMapper extends AbstractModelMapper<OrderPaymentMethod, OrderPaymentMethodDto> {

    @Autowired
    public OrderPaymentMethodMapper(ObjectMapper mapper) {
        super(mapper, OrderPaymentMethod::new, OrderPaymentMethodDto::new);
    }
}

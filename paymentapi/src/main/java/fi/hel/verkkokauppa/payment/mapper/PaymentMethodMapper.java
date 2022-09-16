package fi.hel.verkkokauppa.payment.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.mapper.AbstractModelMapper;
import fi.hel.verkkokauppa.payment.api.data.PaymentMethodDto;
import fi.hel.verkkokauppa.payment.model.PaymentMethodModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * > This class is a Spring component that maps a PaymentMethodModel object to a PaymentMethodDTO object and back
 */
@Component
public class PaymentMethodMapper extends AbstractModelMapper<PaymentMethodModel, PaymentMethodDto> {

    @Autowired
    public PaymentMethodMapper(ObjectMapper mapper) {
        super(mapper, PaymentMethodModel::new, PaymentMethodDto::new);
    }
}

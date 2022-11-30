package fi.hel.verkkokauppa.payment.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.mapper.AbstractModelMapper;
import fi.hel.verkkokauppa.payment.model.paytrail.payment.PaytrailPaymentProviderModel;
import org.helsinki.paytrail.model.payments.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * > This class is a Spring component that maps a PaymentMethodModel objects to a PaymentMethodDTO objects and back
 */
@Component
public class PaytrailPaymentProviderListMapper extends AbstractModelMapper<PaytrailPaymentProviderModel, Provider> {

    @Autowired
    public PaytrailPaymentProviderListMapper(ObjectMapper mapper) {
        super(mapper, PaytrailPaymentProviderModel::new, Provider::new);
    }
}

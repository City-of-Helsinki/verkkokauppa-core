package fi.hel.verkkokauppa.payment.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.mapper.AbstractModelMapper;
import org.helsinki.paytrail.request.payments.PaytrailPaymentCreateMitChargeRequest.CreateMitChargePayload;
import org.helsinki.paytrail.request.payments.PaytrailPaymentCreateRequest.CreatePaymentPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * > Map create payment payload to create mit charge payload and back
 */
@Component
public class PaytrailCreatePaymentPayloadMapper extends AbstractModelMapper<CreatePaymentPayload, CreateMitChargePayload> {

    @Autowired
    public PaytrailCreatePaymentPayloadMapper(ObjectMapper mapper) {
        super(mapper, CreatePaymentPayload::new, CreateMitChargePayload::new);
    }
}

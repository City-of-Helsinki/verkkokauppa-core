package fi.hel.verkkokauppa.payment.paytrail.converter;

import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;

public interface IPaytrailPayloadConverter<Payload, Source> {

    Payload convertToPayload(PaytrailPaymentContext context, Source source, String stamp);
}

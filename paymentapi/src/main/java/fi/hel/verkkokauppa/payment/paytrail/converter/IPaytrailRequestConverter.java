package fi.hel.verkkokauppa.payment.paytrail.converter;

import fi.hel.verkkokauppa.payment.logic.context.PaytrailPaymentContext;

public interface IPaytrailRequestConverter<Request, Source> {

    Request convertToRequest(PaytrailPaymentContext context, Source source);
}

package fi.hel.verkkokauppa.payment.paytrail.converter.impl;

import fi.hel.verkkokauppa.common.rest.refund.RefundDto;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.paytrail.converter.IPaytrailPayloadConverter;
import fi.hel.verkkokauppa.payment.util.PaymentUtil;
import org.helsinki.paytrail.model.payments.PaymentCallbackUrls;
import org.helsinki.paytrail.request.refunds.PaytrailRefundCreateRequest.CreateRefundPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PaytrailCreateRefundPayloadConverter implements IPaytrailPayloadConverter<CreateRefundPayload, RefundDto> {

    @Autowired
    private Environment env;

    @Override
    public CreateRefundPayload convertToPayload(PaytrailPaymentContext context, RefundDto refundDto, String stamp) {
        CreateRefundPayload payload = new CreateRefundPayload();

        payload.setRefundStamp(stamp);
        payload.setRefundReference(refundDto.getOrderId());
        payload.setAmount(PaymentUtil.convertToCents(new BigDecimal(refundDto.getPriceTotal())).intValue());
        payload.setEmail(refundDto.getCustomerEmail());

        PaymentCallbackUrls callbackUrls = new PaymentCallbackUrls();
        callbackUrls.setSuccess(env.getRequiredProperty("paytrail_refund_success_url"));
        callbackUrls.setCancel(env.getRequiredProperty("paytrail_refund_cancel_url"));
        if (context.getRefundCallbackSuccessUrl() != null && !context.getRefundCallbackSuccessUrl().isEmpty()) {
            callbackUrls.setSuccess(context.getRefundCallbackSuccessUrl());
        }
        if (context.getRefundCallbackCancelUrl() != null && !context.getRefundCallbackCancelUrl().isEmpty()) {
            callbackUrls.setCancel(context.getRefundCallbackCancelUrl());
        }

        payload.setCallbackUrls(callbackUrls);

        return payload;
    }

}

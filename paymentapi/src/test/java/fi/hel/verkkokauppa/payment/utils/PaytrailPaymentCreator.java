package fi.hel.verkkokauppa.payment.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.payment.util.PaymentUtil;
import org.helsinki.paytrail.PaytrailClient;
import org.helsinki.paytrail.mapper.PaytrailPaymentCreateResponseMapper;
import org.helsinki.paytrail.mapper.PaytrailRefundCreateResponseMapper;
import org.helsinki.paytrail.model.payments.PaymentCallbackUrls;
import org.helsinki.paytrail.model.payments.PaymentCustomer;
import org.helsinki.paytrail.model.payments.PaymentItem;
import org.helsinki.paytrail.model.payments.PaytrailPaymentResponse;
import org.helsinki.paytrail.request.payments.PaytrailPaymentCreateRequest;
import org.helsinki.paytrail.response.payments.PaytrailPaymentCreateResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class PaytrailPaymentCreator extends TestPaymentCreator {
    /* Normal test merchant account */
    protected final String merchantId = "375917";
    protected final String secretKey = "SAIPPUAKAUPPIAS";
    protected final PaytrailRefundCreateResponseMapper refundMapper;
    protected final PaytrailPaymentCreateResponseMapper paymentMapper;

    @Autowired
    private Environment env;

    @Autowired
    public PaytrailPaymentCreator() {
        this.refundMapper = new PaytrailRefundCreateResponseMapper(new ObjectMapper());
        this.paymentMapper = new PaytrailPaymentCreateResponseMapper(new ObjectMapper());
    }

    public PaytrailPaymentResponse createTestNormalMerchantPayment(PaytrailClient client, int amount, String stamp) throws ExecutionException, InterruptedException {
        PaytrailPaymentCreateRequest.CreatePaymentPayload payload = new PaytrailPaymentCreateRequest.CreatePaymentPayload();

        payload.setStamp(stamp);
        payload.setReference(client.getInternalMerchantId());
        payload.setAmount(PaymentUtil.eurosToBigInteger(amount).intValue());
        payload.setCurrency("EUR");
        payload.setLanguage("FI");

        PaymentItem paymentItem = new PaymentItem();
        paymentItem.setUnitPrice(PaymentUtil.eurosToBigInteger(amount).intValue());
        paymentItem.setUnits(1);
        paymentItem.setVatPercentage(24);
        paymentItem.setProductCode("#1234");
        ArrayList<PaymentItem> items1 = new ArrayList<>();
        items1.add(paymentItem);
        payload.setItems(items1);

        PaymentCustomer customer = new PaymentCustomer();
        customer.setEmail("test@hiq.fi");
        payload.setCustomer(customer);

        PaymentCallbackUrls callbackUrls = new PaymentCallbackUrls();

        callbackUrls.setSuccess(env.getRequiredProperty("paytrail_payment_return_success_url"));
        callbackUrls.setCancel(env.getRequiredProperty("paytrail_payment_return_cancel_url"));

        payload.setCallbackUrls(callbackUrls);
        payload.setRedirectUrls(callbackUrls);

        PaytrailPaymentCreateRequest request = new PaytrailPaymentCreateRequest(payload);
        CompletableFuture<PaytrailPaymentCreateResponse> response = client.sendRequest(request);

        PaytrailPaymentCreateResponse paymentCreateResponse = paymentMapper.to(response.get());

        return paymentCreateResponse.getPaymentResponse();
    }

}

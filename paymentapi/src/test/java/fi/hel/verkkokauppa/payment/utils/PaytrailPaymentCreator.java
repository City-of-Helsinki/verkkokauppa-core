package fi.hel.verkkokauppa.payment.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class PaytrailPaymentCreator extends TestPaymentCreator{
    /* Normal test merchant account */
    protected final String merchantId = "375917";
    protected final String secretKey = "SAIPPUAKAUPPIAS";
    protected final PaytrailRefundCreateResponseMapper refundMapper;
    protected final PaytrailPaymentCreateResponseMapper paymentMapper;

    @Autowired
    public PaytrailPaymentCreator() {
        this.refundMapper = new PaytrailRefundCreateResponseMapper(new ObjectMapper());
        this.paymentMapper = new PaytrailPaymentCreateResponseMapper(new ObjectMapper());
    }

    public PaytrailPaymentResponse createTestNormalMerchantPayment(PaytrailClient client) throws ExecutionException, InterruptedException {
        PaytrailPaymentCreateRequest.CreatePaymentPayload payload = new PaytrailPaymentCreateRequest.CreatePaymentPayload();

        payload.setStamp(UUID.randomUUID().toString());
        payload.setReference("3759170");
        payload.setAmount(10);
        payload.setCurrency("EUR");
        payload.setLanguage("FI");

        PaymentItem paymentItem = new PaymentItem();
        paymentItem.setUnitPrice(10);
        paymentItem.setUnits(1);
        paymentItem.setVatPercentage(24);
        paymentItem.setProductCode("#1234");
        ArrayList<PaymentItem> items1 = new ArrayList<>();
        items1.add(paymentItem);
        payload.setItems(items1);

        PaymentCustomer customer = new PaymentCustomer();
        customer.setEmail("test@ambientia.fi");
        payload.setCustomer(customer);

        PaymentCallbackUrls callbackUrls = new PaymentCallbackUrls();

        callbackUrls.setSuccess("https://webhook.site/3a2a4e66-3955-4be8-abcb-b56aac026e5f/success");
        callbackUrls.setCancel("https://webhook.site/3a2a4e66-3955-4be8-abcb-b56aac026e5f/cancel");

        payload.setCallbackUrls(callbackUrls);
        payload.setRedirectUrls(callbackUrls);

        PaytrailPaymentCreateRequest request = new PaytrailPaymentCreateRequest(payload);
        CompletableFuture<PaytrailPaymentCreateResponse> response = client.sendRequest(request);

        PaytrailPaymentCreateResponse paymentCreateResponse = paymentMapper.to(response.get());

        return paymentCreateResponse.getPaymentResponse();
    }
}

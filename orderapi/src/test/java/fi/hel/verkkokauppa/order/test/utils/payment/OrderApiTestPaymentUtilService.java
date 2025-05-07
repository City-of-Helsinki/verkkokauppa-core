package fi.hel.verkkokauppa.order.test.utils.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.order.model.Order;
import lombok.RequiredArgsConstructor;
import org.helsinki.paytrail.PaytrailClient;
import org.helsinki.paytrail.model.payments.PaymentCallbackUrls;
import org.helsinki.paytrail.model.payments.PaymentCustomer;
import org.helsinki.paytrail.model.payments.PaytrailPayment;
import org.helsinki.paytrail.request.payments.PaytrailPaymentCreateRequest;
import org.helsinki.paytrail.request.payments.PaytrailPaymentGetRequest;
import org.helsinki.paytrail.request.refunds.PaytrailRefundCreateRequest;
import org.helsinki.paytrail.response.payments.PaytrailPaymentGetResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class OrderApiTestPaymentUtilService {

    public static String SECRET = "SAIPPUAKAUPPIAS";
    public static String ACCOUNT = "375917";

    private final Logger logger = LoggerFactory.getLogger(OrderApiTestPaymentUtilService.class);
    @Autowired
    private ObjectMapper mapper;

    public TestPaytrailPaymentResponse createTestPaytrailPayment(Order order) throws ExecutionException, InterruptedException, JsonProcessingException {
        PaytrailClient paytrailClient = new PaytrailClient(ACCOUNT, SECRET);

        PaytrailPaymentCreateRequest.CreatePaymentPayload payload = new PaytrailPaymentCreateRequest.CreatePaymentPayload();
        payload.setAmount(convertToCents(order.getPriceTotal()));
        payload.setLanguage("FI");
        payload.setCurrency("EUR");

        PaymentCustomer paymentCustomer = new PaymentCustomer();
        paymentCustomer.setEmail(order.getCustomerEmail());
        payload.setCustomer(paymentCustomer);

        PaymentCallbackUrls callbackUrls = new PaymentCallbackUrls();
        callbackUrls.setSuccess("https://example.test:8285/v1/payment/paytrailOnlinePayment/notify/success");
        callbackUrls.setCancel("https://example.test:8285/v1/payment/paytrailOnlinePayment/notify/cancel");
        payload.setCallbackUrls(callbackUrls);

        PaymentCallbackUrls redirectUrls = new PaymentCallbackUrls();
        redirectUrls.setSuccess("https://example.test:8285/v1/payment/paytrailOnlinePayment/return/success");
        redirectUrls.setCancel("https://example.test:8285/v1/payment/paytrailOnlinePayment/return/cancel");
        payload.setRedirectUrls(redirectUrls);
        payload.setStamp(order.getOrderId()); // TODO should be unique?
        payload.setReference(order.getOrderId() + "_" + UUID.randomUUID().toString());
        PaytrailPaymentCreateRequest request = new PaytrailPaymentCreateRequest(payload);
        return mapper.readValue(paytrailClient.sendRequest(request).get().getResultJson(), TestPaytrailPaymentResponse.class);
    }


    public PaytrailPayment getTestPaytrailPayment(String paytrailTransactionId) throws ExecutionException, InterruptedException, JsonProcessingException {
        PaytrailClient paytrailClient = new PaytrailClient(ACCOUNT, SECRET);

        PaytrailPaymentGetRequest request = new PaytrailPaymentGetRequest(paytrailTransactionId);
        String resultJson = paytrailClient.sendRequest(request).get().getResultJson();
        return mapper.readValue(resultJson, PaytrailPayment.class);
    }


    public TestPaytrailRefundResponse createTestPaytrailRefund(Order order, TestPayment testPayment) throws ExecutionException, InterruptedException, JsonProcessingException {
        PaytrailClient paytrailClient = new PaytrailClient(ACCOUNT, SECRET);

        PaytrailRefundCreateRequest.CreateRefundPayload createRefundPayload = new PaytrailRefundCreateRequest.CreateRefundPayload();
        createRefundPayload.setAmount(convertToCents(String.valueOf(testPayment.getTotal())));;
        createRefundPayload.setEmail(order.getCustomerEmail());

        // Todo needs NGROK to make them work for local
        PaymentCallbackUrls callbackUrls = new PaymentCallbackUrls();
        callbackUrls.setSuccess("https://example.test:8285/v1/payment/paytrailOnlineRefund/success");
        callbackUrls.setCancel("https://example.test:8285/v1/payment/paytrailOnlineRefund/cancel");
        createRefundPayload.setCallbackUrls(callbackUrls);

        String refundStamp = order.getOrderId() + "_" + UUID.randomUUID().toString();
        createRefundPayload.setRefundStamp(refundStamp); // TODO should be unique?
        createRefundPayload.setRefundReference(refundStamp);
        PaytrailRefundCreateRequest request = new PaytrailRefundCreateRequest(
                testPayment.getPaytrailTransactionId(),
                createRefundPayload
        );

        String resultJson = paytrailClient.sendRequest(request).get().getResultJson();
        return mapper.readValue(resultJson, TestPaytrailRefundResponse.class);
    }



    private int convertToCents(String euroAmount) {
        try {
            return (int) Math.round(Double.parseDouble(euroAmount) * 100);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid price format: " + euroAmount);
        }
    }
}

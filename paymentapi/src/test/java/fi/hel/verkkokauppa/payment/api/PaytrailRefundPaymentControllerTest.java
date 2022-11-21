package fi.hel.verkkokauppa.payment.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.payment.api.data.refund.RefundRequestDataDto;
import fi.hel.verkkokauppa.payment.testing.annotations.RunIfProfile;
import fi.hel.verkkokauppa.payment.utils.PaytrailPaymentCreator;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.PaytrailClient;
import org.helsinki.paytrail.constants.RefundStatus;
import org.helsinki.paytrail.model.payments.PaymentCallbackUrls;
import org.helsinki.paytrail.model.payments.PaytrailPaymentResponse;
import org.helsinki.paytrail.request.refunds.PaytrailRefundCreateRequest;
import org.helsinki.paytrail.response.refunds.PaytrailRefundCreateResponse;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@RunIfProfile(profile = "local")
@Slf4j
public class PaytrailRefundPaymentControllerTest extends PaytrailPaymentCreator {

    @Autowired
    private PaytrailRefundPaymentController paytrailRefundPaymentController;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void createRefundPaymentFromRefund() throws ExecutionException, InterruptedException, JsonProcessingException {
        RefundRequestDataDto dto = new RefundRequestDataDto();
        PaytrailClient client = new PaytrailClient(merchantId, secretKey);
        PaytrailPaymentResponse paymentResponse = createTestNormalMerchantPayment(client);
        // Manual step for testing -> go to href and then use nordea to approve payment
        log.info(paymentResponse.getHref());
        log.info(paymentResponse.getTransactionId());

        PaytrailRefundCreateRequest.CreateRefundPayload payload = new PaytrailRefundCreateRequest.CreateRefundPayload();


        String refundStamp = UUID.randomUUID().toString();
        String refundReference = "3759170";

        payload.setRefundStamp(refundStamp);
        payload.setRefundReference(refundReference);
        payload.setAmount(10);
        payload.setEmail("martin.lehtomaa@ambientia.fi");

        PaymentCallbackUrls callbackUrls = new PaymentCallbackUrls();

        callbackUrls.setSuccess("https://ecom.example.com/cart/success");
        callbackUrls.setCancel("https://ecom.example.com/cart/cancel");
        payload.setCallbackUrls(callbackUrls);

        // Set debugger to stop here, and approve payment manually using ui.
        PaytrailRefundCreateRequest request = new PaytrailRefundCreateRequest(paymentResponse.getTransactionId(), payload);
        CompletableFuture<PaytrailRefundCreateResponse> response = client.sendRequest(request);

        PaytrailRefundCreateResponse refundCreateResponse = refundMapper.to(response.get());
        log.info(objectMapper.writeValueAsString(refundCreateResponse.getRefundResponse()));
        Assertions.assertEquals(refundCreateResponse.getRefundResponse().getStatus(), RefundStatus.OK);
        // Payment creation transactionId is different from refundCreation response.
        Assertions.assertNotEquals(refundCreateResponse.getRefundResponse().getTransactionId(), paymentResponse.getTransactionId());
        paytrailRefundPaymentController.createRefundPaymentFromRefund(dto);
    }
}
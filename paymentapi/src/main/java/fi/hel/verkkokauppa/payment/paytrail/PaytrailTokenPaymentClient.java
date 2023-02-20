package fi.hel.verkkokauppa.payment.paytrail;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.rest.refund.RefundDto;
import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.paytrail.converter.IPaytrailPayloadConverter;
import fi.hel.verkkokauppa.payment.paytrail.factory.PaytrailAuthClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.PaytrailClient;
import org.helsinki.paytrail.mapper.PaytrailPaymentCreateMitChargeResponseMapper;
import org.helsinki.paytrail.model.payments.PaytrailPaymentResponse;
import org.helsinki.paytrail.request.payments.PaytrailPaymentCreateMitChargeRequest;
import org.helsinki.paytrail.request.payments.PaytrailPaymentCreateRequest;
import org.helsinki.paytrail.request.payments.PaytrailPaymentGetRequest;
import org.helsinki.paytrail.request.refunds.PaytrailRefundCreateRequest;
import org.helsinki.paytrail.response.payments.PaytrailPaymentCreateMitChargeResponse;
import org.helsinki.paytrail.response.payments.PaytrailPaymentGetResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class PaytrailTokenPaymentClient {

    private final PaytrailAuthClientFactory paytrailAuthClientFactory;
    private final PaytrailPaymentCreateMitChargeResponseMapper paymentCreateResponseMapper;
    private final IPaytrailPayloadConverter<PaytrailPaymentCreateMitChargeRequest.CreateMitChargePayload, OrderWrapper> paymentPayloadConverter;

    @Autowired
    public PaytrailTokenPaymentClient(
            PaytrailAuthClientFactory paytrailAuthClientFactory,
            ObjectMapper mapper,
            IPaytrailPayloadConverter<PaytrailPaymentCreateMitChargeRequest.CreateMitChargePayload, OrderWrapper> PaytrailCreateTokenPaymentPayloadConverter,
            IPaytrailPayloadConverter<PaytrailRefundCreateRequest.CreateRefundPayload, RefundDto> paytrailCreateRefundPayloadConverter
    ) {
        this.paytrailAuthClientFactory = paytrailAuthClientFactory;
        this.paymentCreateResponseMapper = new PaytrailPaymentCreateMitChargeResponseMapper(mapper);
        this.paymentPayloadConverter = PaytrailCreateTokenPaymentPayloadConverter;
    }

    public PaytrailPaymentCreateMitChargeResponse createPaymentWithToken(PaytrailPaymentContext context, String paymentId, OrderWrapper orderWrapperDto, Payment payment, String token) {
        PaytrailClient paytrailClient = paytrailAuthClientFactory.createPaytrailClientFromPaymentContext(context);

        PaytrailPaymentCreateMitChargeResponse postResponse;
        PaytrailPaymentGetResponse getResponse;
        PaytrailPaymentCreateMitChargeResponse mappedPostResponse;

        try {
            PaytrailPaymentCreateMitChargeRequest.CreateMitChargePayload payload = paymentPayloadConverter.convertToPayload(context, orderWrapperDto, paymentId);
            payload.setToken(token);
            // call token/mit/charge
            PaytrailPaymentCreateMitChargeRequest paymentPostRequest = new PaytrailPaymentCreateMitChargeRequest(payload);
            CompletableFuture<PaytrailPaymentCreateMitChargeResponse> asyncPostResponse = paytrailClient.sendRequest(paymentPostRequest);
            postResponse = asyncPostResponse.get();
            mappedPostResponse = paymentCreateResponseMapper.to(postResponse);

            // if failed throw exception
            if (mappedPostResponse.getSuccess() == null) {
                if (mappedPostResponse.getErrors().length > 0 || mappedPostResponse.getErrors() != null) {
                    log.info("createPaymentWithToken errors {}", (Object) postResponse.getErrors());
                }
                throw new IllegalArgumentException("createPaymentWithToken failed with response : " + mappedPostResponse.getResultJson());
            }

            // save transaction id and also use it to call  HTTP GET /payments/{transactionId}
            payment.setPaytrailTransactionId(mappedPostResponse.getSuccess().getTransactionId());
            PaytrailPaymentGetRequest paymentGetRequest = new PaytrailPaymentGetRequest(payment.getPaytrailTransactionId());
            CompletableFuture<PaytrailPaymentGetResponse> asyncGetResponse = paytrailClient.sendRequest(paymentGetRequest);
            getResponse = asyncGetResponse.get();

            // set payment data
            payment.setPaymentId(paymentId);
            payment.setNamespace(orderWrapperDto.getOrder().getNamespace());


        } catch (InterruptedException | ExecutionException | IllegalArgumentException e) {
            log.debug("Something went wrong in paytrail payment creation: {}", e.getMessage());
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-create-paytrail-payment", "Failed to create paytrail payment")
            );
        }
        return mappedPostResponse;
    }
}

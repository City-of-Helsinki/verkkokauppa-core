package fi.hel.verkkokauppa.payment.paytrail;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.rest.refund.RefundDto;
import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;
import fi.hel.verkkokauppa.payment.mapper.PaytrailCreatePaymentPayloadMapper;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.paytrail.converter.IPaytrailPayloadConverter;
import fi.hel.verkkokauppa.payment.paytrail.factory.PaytrailAuthClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.PaytrailClient;
import org.helsinki.paytrail.mapper.PaytrailGetTokenResponseMapper;
import org.helsinki.paytrail.mapper.PaytrailPaymentCreateMitChargeResponseMapper;
import org.helsinki.paytrail.mapper.PaytrailPaymentCreateResponseMapper;
import org.helsinki.paytrail.mapper.PaytrailPaymentMethodsResponseMapper;
import org.helsinki.paytrail.model.paymentmethods.PaytrailPaymentMethod;
import org.helsinki.paytrail.model.payments.PaytrailPaymentMitChargeSuccessResponse;
import org.helsinki.paytrail.model.payments.PaytrailPaymentResponse;
import org.helsinki.paytrail.model.tokenization.PaytrailTokenResponse;
import org.helsinki.paytrail.request.paymentmethods.PaytrailPaymentMethodsRequest;
import org.helsinki.paytrail.request.payments.PaytrailPaymentCreateMitChargeRequest;
import org.helsinki.paytrail.request.payments.PaytrailPaymentCreateRequest;
import org.helsinki.paytrail.request.payments.PaytrailPaymentCreateRequest.CreatePaymentPayload;
import org.helsinki.paytrail.request.refunds.PaytrailRefundCreateRequest.CreateRefundPayload;
import org.helsinki.paytrail.request.tokenization.PaytrailGetTokenRequest;
import org.helsinki.paytrail.response.paymentmethods.PaytrailPaymentMethodsResponse;
import org.helsinki.paytrail.response.payments.PaytrailPaymentCreateMitChargeResponse;
import org.helsinki.paytrail.response.payments.PaytrailPaymentCreateResponse;
import org.helsinki.paytrail.response.tokenization.PaytrailGetTokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class PaytrailPaymentClient {

    @Value("${paytrail_callback_delay:60}")
    private int callbackDelay;

    private final PaytrailAuthClientFactory paytrailAuthClientFactory;
    private final PaytrailPaymentMethodsResponseMapper paymentMethodsResponseMapper;
    private final PaytrailPaymentCreateResponseMapper paymentCreateResponseMapper;
    private final IPaytrailPayloadConverter<CreatePaymentPayload, OrderWrapper> paymentPayloadConverter;

    private final PaytrailGetTokenResponseMapper getTokenResponseMapper;

    private final PaytrailPaymentCreateMitChargeResponseMapper createMitChargeResponseMapper;
    private final PaytrailCreatePaymentPayloadMapper paytrailCreatePaymentPayloadMapper;
    private final IPaytrailPayloadConverter<CreatePaymentPayload, OrderMessage> createPaymentPayloadFromOrderMessage;

    @Autowired
    public PaytrailPaymentClient(
            PaytrailAuthClientFactory paytrailAuthClientFactory,
            ObjectMapper mapper,
            IPaytrailPayloadConverter<CreatePaymentPayload, OrderWrapper> paytrailCreatePaymentPayloadConverter,
            IPaytrailPayloadConverter<CreatePaymentPayload, OrderMessage> paytrailCreatePaymentPayloadFromOrderMessage,
            IPaytrailPayloadConverter<CreateRefundPayload, RefundDto> paytrailCreateRefundPayloadConverter,
            PaytrailCreatePaymentPayloadMapper paytrailCreatePaymentPayloadMapper
    ) {
        this.paytrailAuthClientFactory = paytrailAuthClientFactory;
        this.paymentMethodsResponseMapper = new PaytrailPaymentMethodsResponseMapper(mapper);
        this.paymentCreateResponseMapper = new PaytrailPaymentCreateResponseMapper(mapper);
        this.paymentPayloadConverter = paytrailCreatePaymentPayloadConverter;
        this.getTokenResponseMapper = new PaytrailGetTokenResponseMapper(mapper);
        this.createMitChargeResponseMapper = new PaytrailPaymentCreateMitChargeResponseMapper(mapper);
        this.paytrailCreatePaymentPayloadMapper = paytrailCreatePaymentPayloadMapper;
        this.createPaymentPayloadFromOrderMessage = paytrailCreatePaymentPayloadFromOrderMessage;
    }

    public List<PaytrailPaymentMethod> getPaymentMethods(PaytrailPaymentContext context) {
        PaytrailClient paytrailClient = paytrailAuthClientFactory.createPaytrailClientFromPaymentContext(context);

        PaytrailPaymentMethodsRequest.PaymentMethodsPayload payload = new PaytrailPaymentMethodsRequest.PaymentMethodsPayload();
        PaytrailPaymentMethodsRequest request = new PaytrailPaymentMethodsRequest(payload);
        CompletableFuture<PaytrailPaymentMethodsResponse> response = paytrailClient.sendRequest(request);

        try {
            PaytrailPaymentMethodsResponse methodsResponse = paymentMethodsResponseMapper.to(response.get());
            return methodsResponse.getPaymentMethods();
        } catch (ExecutionException | InterruptedException | RuntimeException e) {
            log.warn("getting paytrail payment methods failed ", e);
            return Collections.emptyList();
        }
    }

    private PaytrailPaymentMitChargeSuccessResponse createMitCharge(PaytrailClient client, PaytrailPaymentCreateMitChargeRequest.CreateMitChargePayload payload) throws ExecutionException, InterruptedException {
        payload.setCallbackDelay(callbackDelay);
        PaytrailPaymentCreateMitChargeRequest request = new PaytrailPaymentCreateMitChargeRequest(payload);
        CompletableFuture<PaytrailPaymentCreateMitChargeResponse> response = client.sendRequest(request);
        PaytrailPaymentCreateMitChargeResponse createResponse = createMitChargeResponseMapper.to(response.get());
        if (!createResponse.isValid()) {
            log.info("createMitCharge errors {}", createResponse);
            throw new IllegalArgumentException("createMitCharge failed with response : " + createResponse.getResultJson());
        }
        return createResponse.getSuccess();
    }

    public PaytrailPaymentMitChargeSuccessResponse createMitCharge(PaytrailPaymentContext context, String paymentId, OrderWrapper orderWrapperDto, String token) throws ExecutionException, InterruptedException {
        PaytrailClient paytrailClient = paytrailAuthClientFactory.createPaytrailClientFromPaymentContext(context);
        PaytrailPaymentCreateMitChargeRequest.CreateMitChargePayload payload = paytrailCreatePaymentPayloadMapper.toDto(paymentPayloadConverter.convertToPayload(context, orderWrapperDto, paymentId));
        payload.setCallbackDelay(callbackDelay);
        payload.setToken(token);
        return createMitCharge(paytrailClient, payload);
    }

    public PaytrailPaymentMitChargeSuccessResponse createMitCharge(PaytrailPaymentContext context, String paymentId, OrderMessage message, String token) throws ExecutionException, InterruptedException {
        PaytrailClient paytrailClient = paytrailAuthClientFactory.createPaytrailClientFromPaymentContext(context);
        PaytrailPaymentCreateMitChargeRequest.CreateMitChargePayload payload = paytrailCreatePaymentPayloadMapper.toDto(createPaymentPayloadFromOrderMessage.convertToPayload(context, message, paymentId));
        payload.setToken(token);
        payload.setCallbackDelay(callbackDelay);
        return createMitCharge(paytrailClient, payload);
    }

    public PaytrailPaymentResponse createPayment(PaytrailPaymentContext context, String paymentId, OrderWrapper orderWrapperDto) {
        PaytrailClient paytrailClient = paytrailAuthClientFactory.createPaytrailClientFromPaymentContext(context);

        CreatePaymentPayload payload = paymentPayloadConverter.convertToPayload(context, orderWrapperDto, paymentId);
        payload.setCallbackDelay(callbackDelay);
        PaytrailPaymentCreateRequest request = new PaytrailPaymentCreateRequest(payload);
        CompletableFuture<PaytrailPaymentCreateResponse> response = paytrailClient.sendRequest(request);

        try {
            PaytrailPaymentCreateResponse createResponse = paymentCreateResponseMapper.to(response.get());
            if (!createResponse.isValid()) {
                if (createResponse.getErrors().length > 0 || createResponse.getErrors() != null) {
                    log.info("createPayment errors {}", (Object) createResponse.getErrors());
                }
                throw new IllegalArgumentException("createPayment failed with response : " + createResponse.getResultJson());
            }
            return createResponse.getPaymentResponse();
        } catch (InterruptedException | ExecutionException | IllegalArgumentException e) {
            log.debug("Something went wrong in paytrail payment creation: {}", e.getMessage());
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-create-paytrail-payment", "Failed to create paytrail payment")
            );
        }
    }

    public PaytrailTokenResponse getToken(PaytrailPaymentContext context, String tokenizationId) throws ExecutionException, InterruptedException {
        PaytrailClient paytrailClient = paytrailAuthClientFactory.createPaytrailClientFromPaymentContext(context);
        PaytrailGetTokenRequest request = new PaytrailGetTokenRequest(tokenizationId);
        CompletableFuture<PaytrailGetTokenResponse> response = paytrailClient.sendRequest(request);
        PaytrailGetTokenResponse getTokenResponse = getTokenResponseMapper.to(response.get());
        if (!getTokenResponse.isValid()) {
            log.info("getToken errors {}", getTokenResponse);
            throw new IllegalArgumentException("getToken failed with response : " + getTokenResponse.getResultJson());
        }
        return getTokenResponse.getTokenResponse();
    }
}

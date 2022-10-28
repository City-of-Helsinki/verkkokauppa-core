package fi.hel.verkkokauppa.payment.paytrail;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;
import fi.hel.verkkokauppa.payment.logic.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.paytrail.converter.IPaytrailPayloadConverter;
import fi.hel.verkkokauppa.payment.paytrail.factory.PaytrailAuthClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.PaytrailClient;
import org.helsinki.paytrail.mapper.PaytrailPaymentCreateResponseMapper;
import org.helsinki.paytrail.mapper.PaytrailPaymentMethodsResponseMapper;
import org.helsinki.paytrail.model.paymentmethods.PaytrailPaymentMethod;
import org.helsinki.paytrail.model.payments.PaytrailPaymentResponse;
import org.helsinki.paytrail.request.paymentmethods.PaytrailPaymentMethodsRequest;
import org.helsinki.paytrail.request.payments.PaytrailPaymentCreateRequest;
import org.helsinki.paytrail.request.payments.PaytrailPaymentCreateRequest.CreatePaymentPayload;
import org.helsinki.paytrail.response.paymentmethods.PaytrailPaymentMethodsResponse;
import org.helsinki.paytrail.response.payments.PaytrailPaymentCreateResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class PaytrailPaymentClient {

    private final PaytrailAuthClientFactory paytrailAuthClientFactory;
    private final PaytrailPaymentMethodsResponseMapper paymentMethodsResponseMapper;
    private final PaytrailPaymentCreateResponseMapper paymentCreateResponseMapper;
    private final IPaytrailPayloadConverter<CreatePaymentPayload, OrderWrapper> createPaymentPayloadConverter;

    @Autowired
    public PaytrailPaymentClient(
            PaytrailAuthClientFactory paytrailAuthClientFactory,
            ObjectMapper mapper,
            IPaytrailPayloadConverter<CreatePaymentPayload, OrderWrapper> createPaymentPayloadConverter
    ) {
        this.paytrailAuthClientFactory = paytrailAuthClientFactory;
        this.paymentMethodsResponseMapper = new PaytrailPaymentMethodsResponseMapper(mapper);
        this.paymentCreateResponseMapper = new PaytrailPaymentCreateResponseMapper(mapper);
        this.createPaymentPayloadConverter = createPaymentPayloadConverter;
    }

    public List<PaytrailPaymentMethod> getPaymentMethods() {
        PaytrailClient paytrailClient = paytrailAuthClientFactory.getClient();

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

    public PaytrailPaymentResponse createPayment(PaytrailPaymentContext context, String paymentId, OrderWrapper orderWrapperDto) {
        PaytrailClient paytrailClient = paytrailAuthClientFactory.getClient(context.getShopId());

        CreatePaymentPayload payload = createPaymentPayloadConverter.convertToPayload(context, orderWrapperDto);
        payload.setStamp(paymentId);
        PaytrailPaymentCreateRequest request = new PaytrailPaymentCreateRequest(payload);
        CompletableFuture<PaytrailPaymentCreateResponse> response = paytrailClient.sendRequest(request);

        try {
            PaytrailPaymentCreateResponse createResponse = paymentCreateResponseMapper.to(response.get());
            return createResponse.getPaymentResponse();
        } catch (InterruptedException | ExecutionException | IllegalArgumentException e) {
            log.debug("Something went wrong in paytrail payment creation: ", e.getMessage());
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-create-paytrail-payment", "Failed to create paytrail payment")
            );
        }
    }

}

package fi.hel.verkkokauppa.payment.paytrail;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.payment.api.data.OrderWrapper;
import fi.hel.verkkokauppa.common.rest.refund.RefundDto;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.paytrail.converter.IPaytrailPayloadConverter;
import fi.hel.verkkokauppa.payment.paytrail.factory.PaytrailAuthClientFactory;
import fi.hel.verkkokauppa.payment.paytrail.validation.PaymentContextValidator;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.PaytrailClient;
import org.helsinki.paytrail.mapper.PaytrailPaymentCreateResponseMapper;
import org.helsinki.paytrail.mapper.PaytrailPaymentMethodsResponseMapper;
import org.helsinki.paytrail.mapper.PaytrailRefundCreateResponseMapper;
import org.helsinki.paytrail.model.paymentmethods.PaytrailPaymentMethod;
import org.helsinki.paytrail.model.payments.PaytrailPaymentResponse;
import org.helsinki.paytrail.model.refunds.PaytrailRefundResponse;
import org.helsinki.paytrail.request.paymentmethods.PaytrailPaymentMethodsRequest;
import org.helsinki.paytrail.request.payments.PaytrailPaymentCreateRequest;
import org.helsinki.paytrail.request.payments.PaytrailPaymentCreateRequest.CreatePaymentPayload;
import org.helsinki.paytrail.request.refunds.PaytrailRefundCreateRequest;
import org.helsinki.paytrail.request.refunds.PaytrailRefundCreateRequest.CreateRefundPayload;
import org.helsinki.paytrail.response.paymentmethods.PaytrailPaymentMethodsResponse;
import org.helsinki.paytrail.response.payments.PaytrailPaymentCreateResponse;
import org.helsinki.paytrail.response.refunds.PaytrailRefundCreateResponse;
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

    protected final PaytrailAuthClientFactory paytrailAuthClientFactory;
    protected final PaytrailPaymentMethodsResponseMapper paymentMethodsResponseMapper;
    protected final PaytrailPaymentCreateResponseMapper paymentCreateResponseMapper;
    protected final PaytrailRefundCreateResponseMapper refundCreateResponseMapper;
    protected final IPaytrailPayloadConverter<CreatePaymentPayload, OrderWrapper> paymentPayloadConverter;
    protected final IPaytrailPayloadConverter<CreateRefundPayload, RefundDto> refundPayloadConverter;

    @Autowired
    public PaytrailPaymentClient(
            PaytrailAuthClientFactory paytrailAuthClientFactory,
            ObjectMapper mapper,
            IPaytrailPayloadConverter<CreatePaymentPayload, OrderWrapper> paytrailCreatePaymentPayloadConverter,
            IPaytrailPayloadConverter<CreateRefundPayload, RefundDto> paytrailCreateRefundPayloadConverter
    ) {
        this.paytrailAuthClientFactory = paytrailAuthClientFactory;
        this.paymentMethodsResponseMapper = new PaytrailPaymentMethodsResponseMapper(mapper);
        this.paymentCreateResponseMapper = new PaytrailPaymentCreateResponseMapper(mapper);
        this.refundCreateResponseMapper = new PaytrailRefundCreateResponseMapper(mapper);
        this.paymentPayloadConverter = paytrailCreatePaymentPayloadConverter;
        this.refundPayloadConverter = paytrailCreateRefundPayloadConverter;
    }

    public List<PaytrailPaymentMethod> getPaymentMethods(PaytrailPaymentContext context) {
        PaytrailClient paytrailClient = createPaytrailClientFromPaymentContext(context);

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
        PaytrailClient paytrailClient = createPaytrailClientFromPaymentContext(context);

        CreatePaymentPayload payload = paymentPayloadConverter.convertToPayload(context, orderWrapperDto, paymentId);
        PaytrailPaymentCreateRequest request = new PaytrailPaymentCreateRequest(payload);
        CompletableFuture<PaytrailPaymentCreateResponse> response = paytrailClient.sendRequest(request);

        try {
            PaytrailPaymentCreateResponse createResponse = paymentCreateResponseMapper.to(response.get());
            return createResponse.getPaymentResponse();
        } catch (InterruptedException | ExecutionException | IllegalArgumentException e) {
            log.debug("Something went wrong in paytrail payment creation: {}", e.getMessage());
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-create-paytrail-payment", "Failed to create paytrail payment")
            );
        }
    }

    public PaytrailClient createPaytrailClientFromPaymentContext(PaytrailPaymentContext context) {
        PaymentContextValidator.validateContext(context);
        if (context.isUseShopInShop()) {
            return paytrailAuthClientFactory.getShopInShopClient(context.getShopId());
        } else {
            return paytrailAuthClientFactory.getClient(context.getPaytrailMerchantId(), context.getPaytrailSecretKey());
        }
    }

    public PaytrailRefundResponse createRefund(PaytrailPaymentContext context, String refundPaymentId, String paymentTransactionId, RefundDto refundDto) {
        PaytrailClient paytrailClient = createPaytrailClientFromPaymentContext(context);

        CreateRefundPayload payload = refundPayloadConverter.convertToPayload(context, refundDto, refundPaymentId);
        PaytrailRefundCreateRequest request =  new PaytrailRefundCreateRequest(paymentTransactionId, payload);
        CompletableFuture<PaytrailRefundCreateResponse> response = paytrailClient.sendRequest(request);

        try {
            PaytrailRefundCreateResponse createResponse = refundCreateResponseMapper.to(response.get());
            if (!createResponse.isValid()) {
                if (createResponse.getErrors().length > 0) {
                    log.info("createRefund errors {}", (Object) createResponse.getErrors());
                }
                throw new IllegalArgumentException("createRefund failed with response : " + createResponse.getResultJson());
            }
            return createResponse.getRefundResponse();
        } catch (InterruptedException | ExecutionException | IllegalArgumentException e) {
            log.debug("Something went wrong in paytrail refund creation: {}", e.getMessage());
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-create-paytrail-refund", "Failed to create paytrail refund")
            );
        }
    }
}

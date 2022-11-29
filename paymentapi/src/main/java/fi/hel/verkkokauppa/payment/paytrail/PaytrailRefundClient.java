package fi.hel.verkkokauppa.payment.paytrail;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.rest.refund.RefundDto;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.paytrail.converter.IPaytrailPayloadConverter;
import fi.hel.verkkokauppa.payment.paytrail.factory.PaytrailAuthClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.PaytrailClient;
import org.helsinki.paytrail.mapper.PaytrailRefundCreateResponseMapper;
import org.helsinki.paytrail.model.refunds.PaytrailRefundResponse;
import org.helsinki.paytrail.request.refunds.PaytrailRefundCreateRequest;
import org.helsinki.paytrail.request.refunds.PaytrailRefundCreateRequest.CreateRefundPayload;
import org.helsinki.paytrail.response.refunds.PaytrailRefundCreateResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class PaytrailRefundClient {

    private final PaytrailAuthClientFactory paytrailAuthClientFactory;
    private final PaytrailRefundCreateResponseMapper refundCreateResponseMapper;
    private final IPaytrailPayloadConverter<CreateRefundPayload, RefundDto> refundPayloadConverter;

    @Autowired
    public PaytrailRefundClient(
            PaytrailAuthClientFactory paytrailAuthClientFactory,
            ObjectMapper mapper,
            IPaytrailPayloadConverter<CreateRefundPayload, RefundDto> paytrailCreateRefundPayloadConverter
    ) {
        this.paytrailAuthClientFactory = paytrailAuthClientFactory;
        this.refundCreateResponseMapper = new PaytrailRefundCreateResponseMapper(mapper);
        this.refundPayloadConverter = paytrailCreateRefundPayloadConverter;
    }

    public PaytrailRefundResponse createRefund(PaytrailPaymentContext context, String refundPaymentId, String paymentTransactionId, RefundDto refundDto) {
        PaytrailClient paytrailClient = paytrailAuthClientFactory.createPaytrailClientFromPaymentContext(context);

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

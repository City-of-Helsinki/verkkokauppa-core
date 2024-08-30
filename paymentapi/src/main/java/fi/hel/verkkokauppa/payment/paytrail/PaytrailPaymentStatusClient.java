package fi.hel.verkkokauppa.payment.paytrail;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.paytrail.factory.PaytrailAuthClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.PaytrailClient;
import org.helsinki.paytrail.mapper.PaytrailPaymentGetResponseMapper;
import org.helsinki.paytrail.model.payments.PaytrailPayment;
import org.helsinki.paytrail.request.payments.PaytrailPaymentGetRequest;
import org.helsinki.paytrail.response.payments.PaytrailPaymentGetResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class PaytrailPaymentStatusClient {

    private final PaytrailAuthClientFactory paytrailAuthClientFactory;
    private final PaytrailPaymentGetResponseMapper getPaymentMapper;

    @Autowired
    public PaytrailPaymentStatusClient(
            PaytrailAuthClientFactory paytrailAuthClientFactory,
            ObjectMapper mapper
    ) {
        this.paytrailAuthClientFactory = paytrailAuthClientFactory;
        this.getPaymentMapper = new PaytrailPaymentGetResponseMapper(mapper);
    }

    public PaytrailPayment getPaytrailPayment(PaytrailPaymentContext context, String paytrailTransactionId) {
        PaytrailClient paytrailClient = paytrailAuthClientFactory.createPaytrailClientFromPaymentContext(context);

        PaytrailPaymentGetRequest request = new PaytrailPaymentGetRequest(paytrailTransactionId);
        CompletableFuture<PaytrailPaymentGetResponse> response = paytrailClient.sendRequest(request);
        try {
            PaytrailPaymentGetResponse paymentGetResponse = getPaymentMapper.to(response.get());
            return paymentGetResponse.getPaytrailPayment();
        } catch (ExecutionException | InterruptedException | RuntimeException e) {
            log.warn(MessageFormat.format("getting paytrail payment with paytrailTransactionId {0} failed ", paytrailTransactionId), e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-paytrail-payment-with-paytrail-transaction-id", "Failed to get paytrail payment with paytrailTransactionId " + paytrailTransactionId)
            );
        }
    }

}

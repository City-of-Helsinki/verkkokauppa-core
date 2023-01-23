package fi.hel.verkkokauppa.payment.paytrail;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.paytrail.factory.PaytrailAuthClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.PaytrailClient;
import org.helsinki.paytrail.mapper.PaytrailGetTokenResponseMapper;
import org.helsinki.paytrail.model.tokenization.PaytrailTokenResponse;
import org.helsinki.paytrail.request.tokenization.PaytrailGetTokenRequest;
import org.helsinki.paytrail.response.tokenization.PaytrailGetTokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class PaytrailTokenPaymentClient {

    private final PaytrailAuthClientFactory paytrailAuthClientFactory;
    private final PaytrailGetTokenResponseMapper tokenResponseMapper;

    @Autowired
    public PaytrailTokenPaymentClient(
            PaytrailAuthClientFactory paytrailAuthClientFactory,
            ObjectMapper mapper
    ) {
        this.paytrailAuthClientFactory = paytrailAuthClientFactory;
        this.tokenResponseMapper = new PaytrailGetTokenResponseMapper(mapper);
    }

    public PaytrailTokenResponse getCardTokenForPayment(PaytrailPaymentContext context, String checkoutTokenizationId) {
        PaytrailClient paytrailClient = paytrailAuthClientFactory.createPaytrailClientFromPaymentContext(context);

        PaytrailGetTokenRequest request = new PaytrailGetTokenRequest(checkoutTokenizationId);
        CompletableFuture<PaytrailGetTokenResponse> response = paytrailClient.sendRequest(request);

        try {
            PaytrailGetTokenResponse tokenResponse = tokenResponseMapper.to(response.get());
            return tokenResponse.getTokenResponse();
        } catch (ExecutionException | InterruptedException | RuntimeException e) {
            log.warn("getting paytrail card token failed ", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-paytrail-card-token", "Failed to get paytrail card token")
            );
        }
    }

}

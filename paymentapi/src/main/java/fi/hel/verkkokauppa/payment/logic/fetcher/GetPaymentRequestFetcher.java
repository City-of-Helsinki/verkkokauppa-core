package fi.hel.verkkokauppa.payment.logic.fetcher;

import fi.hel.verkkokauppa.payment.logic.fetcher.base.BaseFetcher;
import fi.hel.verkkokauppa.payment.logic.visma.VismaAuth;
import org.helsinki.vismapay.VismaPayClient;
import org.helsinki.vismapay.request.payment.GetPaymentRequest;
import org.helsinki.vismapay.response.payment.PaymentDetailsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
public class GetPaymentRequestFetcher extends BaseFetcher {
	@Autowired
	private VismaAuth vismaAuth;
    private Logger log = LoggerFactory.getLogger(GetPaymentRequestFetcher.class);

	public PaymentDetailsResponse getPaymentDetails(String paymentId) {

		VismaPayClient client = vismaAuth.getClient();
		CompletableFuture<PaymentDetailsResponse> responseCF = client.sendRequest(
				this.getGetPaymentRequest(paymentId)
		);

		try {
			PaymentDetailsResponse response = responseCF.get();
			if (isSuccessResponse(response)) {
				return response;
			} else {
				log.error("{} request failed, check application.properties", getMethodName());
				log.debug("Visma {} error response {}", getMethodName(), response);
				throw new RuntimeException(buildErrorMsg(response));
			}
		} catch (InterruptedException | ExecutionException e) {
			log.error("exception when getting payment details from visma", e);
			throw new RuntimeException("Got the following exception: " + e.getMessage());
		}
	}

	private GetPaymentRequest getGetPaymentRequest(String paymentId) {
		GetPaymentRequest.GetPaymentPayload payload = new GetPaymentRequest.GetPaymentPayload();
		payload.setOrderNumber(paymentId);
		return new GetPaymentRequest(payload);
	}

	protected String buildErrorMsg(PaymentDetailsResponse response) {
		String errorMsg = "Unable to get payment request ";
		if (response == null) {
			throw new IllegalArgumentException("Response can't be null.");
		}

		if (response.getResult() != 0) {
			if (response.getErrors() != null && response.getErrors().length > 0) {
				return errorMsg + "Validation errors: " + String.join(", ", response.getErrors());
			}
			return errorMsg + "Please check that api key and private key are correct.";
		}

		throw new IllegalArgumentException("Response was successful. Should never get here if called properly.");
	}

}

package fi.hel.verkkokauppa.payment.logic;

import org.helsinki.vismapay.VismaPayClient;
import org.helsinki.vismapay.request.payment.ChargeRequest;
import org.helsinki.vismapay.response.payment.ChargeResponse;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
public class TokenFetcher {

	public String getToken(ChargeRequest.PaymentTokenPayload payload) {
		VismaPayClient client = new VismaPayClient("api_key", "private_key"); // TODO: replace

		CompletableFuture<ChargeResponse> responseCF =
				client.sendRequest(new ChargeRequest(payload));

		try {
			ChargeResponse response = responseCF.get();
			if (response.getResult() == 0) {
				return response.getToken();
			} else {
				throw new RuntimeException(buildErrorMsg(response));
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException("Got the following exception: " + e.getMessage());
		}
	}

	private String buildErrorMsg(ChargeResponse response) {
		String errorMsg = "Unable to create a payment. ";
		if (response == null) {
			throw new IllegalArgumentException("Response can't be null.");
		}

		if (response.getResult() != 0) {
			if (response.getErrors().length > 0) {
				return errorMsg + "Validation errors: " + String.join(", ", response.getErrors());
			}
			return errorMsg + "Please check that api key and private key are correct.";
		}

		throw new IllegalArgumentException("Response was successful. Should never get here if called properly.");
	}
}

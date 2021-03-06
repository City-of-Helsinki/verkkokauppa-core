package fi.hel.verkkokauppa.payment.logic.fetcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.helsinki.vismapay.VismaPayClient;
import org.helsinki.vismapay.request.payment.ChargeRequest;
import org.helsinki.vismapay.response.payment.ChargeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
public class PaymentTokenFetcher {
    
    private Logger log = LoggerFactory.getLogger(PaymentTokenFetcher.class);
	@Autowired
	private ObjectMapper mapper;
    @Autowired
    private Environment env;

	public String getToken(ChargeRequest.PaymentTokenPayload payload) {
		String apiKey = env.getRequiredProperty("payment_api_key");
		String encryptionKey = env.getRequiredProperty("payment_encryption_key");
		String apiVersion = env.getRequiredProperty("payment_transaction_api_version");

		VismaPayClient client = new VismaPayClient(apiKey, encryptionKey, apiVersion);

		ChargeRequest request = new ChargeRequest(payload);
		// TODO remove later?
		try {
			log.debug("getToken ChargeRequest request : {}", mapper.writeValueAsString(payload));
		} catch (JsonProcessingException e) {
			log.error(e.getMessage());
		}

		CompletableFuture<ChargeResponse> responseCF =
				client.sendRequest(request);

		try {
			ChargeResponse response = responseCF.get();
			if (response.getResult() == 0) {
				return response.getToken();
			} else {
				log.error("payment token request failed, check application.properties");
				log.debug("Visma getToken error response {}", response);
				throw new RuntimeException(buildErrorMsg(response));
			}
		} catch (InterruptedException | ExecutionException e) {
			log.error("exception when getting payment token", e);
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

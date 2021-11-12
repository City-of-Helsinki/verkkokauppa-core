package fi.hel.verkkokauppa.payment.logic;

import org.helsinki.vismapay.VismaPayClient;
import org.helsinki.vismapay.request.payment.ChargeCardTokenRequest;
import org.helsinki.vismapay.response.payment.ChargeCardTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
public class ChargeCardTokenLogic {
    
    private Logger log = LoggerFactory.getLogger(ChargeCardTokenLogic.class);

    @Autowired
    private Environment env;

	public String chargeCardToken(ChargeCardTokenRequest.CardTokenPayload payload) {
		String apiKey = env.getRequiredProperty("payment_api_key");
		String encryptionKey = env.getRequiredProperty("payment_encryption_key");
		String apiVersion = env.getRequiredProperty("payment_transaction_api_version");

		VismaPayClient client = new VismaPayClient(apiKey, encryptionKey, apiVersion);

		log.debug("Card token payload: {}", payload);
		CompletableFuture<ChargeCardTokenResponse> responseCF =
				client.sendRequest(new ChargeCardTokenRequest(payload));

		try {
			ChargeCardTokenResponse response = responseCF.get();
			if (response.getResult() == 0) {
				return response.getSettled().toString();
			} else {
				log.error("card token charge request failed, check application.properties");
				throw new RuntimeException(buildErrorMsg(response));
			}
		} catch (InterruptedException | ExecutionException e) {
			log.error("exception when charging card token", e);
			throw new RuntimeException("Got the following exception: " + e.getMessage());
		}
	}

	private String buildErrorMsg(ChargeCardTokenResponse response) {
		String errorMsg = "Unable to charge card token. ";
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

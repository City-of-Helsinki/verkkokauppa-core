package fi.hel.verkkokauppa.payment.logic.fetcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.payment.api.data.PaymentCardInfoDto;
import org.helsinki.vismapay.VismaPayClient;
import org.helsinki.vismapay.request.payment.CheckPaymentStatusRequest;
import org.helsinki.vismapay.response.payment.PaymentStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
public class CardTokenFetcher {
    
    private Logger log = LoggerFactory.getLogger(CardTokenFetcher.class);
	@Autowired
    private ObjectMapper mapper;
    @Autowired
    private Environment env;

	public PaymentCardInfoDto getCardToken(String paymentId) {
		String apiKey = env.getRequiredProperty("payment_api_key");
		String encryptionKey = env.getRequiredProperty("payment_encryption_key");
		String apiVersion = env.getRequiredProperty("payment_transaction_api_version");

		VismaPayClient client = new VismaPayClient(apiKey, encryptionKey, apiVersion);

		CheckPaymentStatusRequest.PaymentStatusPayload payload = buildPayloadFor(paymentId);
		CheckPaymentStatusRequest request = new CheckPaymentStatusRequest(payload);
		// TODO remove later?
		try {
			log.debug("getCardToken CheckPaymentStatusRequest request : {}", mapper.writeValueAsString(payload));
		} catch (JsonProcessingException e) {
			log.error(e.getMessage());
		}

		CompletableFuture<PaymentStatusResponse> responseCF =
				client.sendRequest(request);

		try {
			PaymentStatusResponse response = responseCF.get();
			if (response.getResult() == 0) {
				PaymentCardInfoDto paymentCardInfoDto = new PaymentCardInfoDto();
				paymentCardInfoDto.setCardToken(response.getSource().getCardToken());
				paymentCardInfoDto.setExpYear(response.getSource().getExpYear());
				paymentCardInfoDto.setExpMonth(response.getSource().getExpMonth());
				paymentCardInfoDto.setCardLastFourDigits(response.getSource().getLast4());

				return paymentCardInfoDto;
			} else {
				log.error("card payment token request failed, check application.properties");
				log.debug("Visma getCardToken error response {}", response);
				throw new RuntimeException(buildErrorMsg(response));
			}
		} catch (InterruptedException | ExecutionException e) {
			log.error("exception when getting card token", e);
			throw new RuntimeException("Got the following exception: " + e.getMessage());
		}
	}

	private CheckPaymentStatusRequest.PaymentStatusPayload buildPayloadFor(String paymentId) {
		CheckPaymentStatusRequest.PaymentStatusPayload payload
				= new CheckPaymentStatusRequest.PaymentStatusPayload();
		payload.setOrderNumber(paymentId);
		return payload;
	}

	private String buildErrorMsg(PaymentStatusResponse response) {
		String errorMsg = "Unable to fetch card token. ";
		if (response == null) {
			throw new IllegalArgumentException("Response can't be null.");
		}

		if (response.getResult() != 0) {
			return errorMsg + "Please check that api key and private key are correct.";
		}

		throw new IllegalArgumentException("Response was successful. Should never get here if called properly.");
	}
}

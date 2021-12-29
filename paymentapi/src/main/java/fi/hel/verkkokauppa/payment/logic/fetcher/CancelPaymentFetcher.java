package fi.hel.verkkokauppa.payment.logic.fetcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import fi.hel.verkkokauppa.payment.logic.fetcher.base.BaseFetcher;
import fi.hel.verkkokauppa.payment.logic.visma.VismaAuth;
import org.helsinki.vismapay.VismaPayClient;
import org.helsinki.vismapay.request.payment.CancelPaymentRequest;
import org.helsinki.vismapay.response.VismaPayResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
public class CancelPaymentFetcher extends BaseFetcher {
	@Autowired
	private VismaAuth vismaAuth;
	private Logger log = LoggerFactory.getLogger(CancelPaymentFetcher.class);

	public VismaPayResponse cancelPayment(String paymentId) throws JsonProcessingException {

		VismaPayClient client = vismaAuth.getClient();
		CancelPaymentRequest request = this.getCancelPaymentRequest(paymentId);
		CompletableFuture<VismaPayResponse> responseCF = client.sendRequest(
				request
		);
		log.debug("payload {}",getMapper().writeValueAsString(request));
		try {
			VismaPayResponse response = responseCF.get();
			if (isSuccessResponse(response)) {
				return response;
			} else {
				log.error("cancelPayment request failed, check application.properties");
				log.debug("Visma cancelPayment error response {}", response);
				throw new RuntimeException(buildErrorMsg(response,paymentId));
			}
		} catch (InterruptedException | ExecutionException e) {
			log.error("exception when getting payment details from visma", e);
			throw new RuntimeException("Got the following exception: " + e.getMessage());
		}
	}

	private CancelPaymentRequest getCancelPaymentRequest(String paymentId) {
		CancelPaymentRequest.CancelPaymentPayload payload = new CancelPaymentRequest.CancelPaymentPayload();
		// Order number of the payment to cancel. -> paymentId
		payload.setOrderNumber(paymentId);
		return new CancelPaymentRequest(payload);
	}

	private String buildErrorMsg(VismaPayResponse response, String paymentId) {
		String errorMsg = "Unable to cancel a payment. ";
		if (response == null) {
			throw new IllegalArgumentException("Response can't be null.");
		}
		if (isSuccessResponse(response)) {
			switch (response.getResult()) {
				case 1:
					log.debug("Cancel request for paymentId: {} result code was {} Validation Error", paymentId, response.getResult());
					break;
				case 2:
					log.debug("Cancel request for paymentId: {} result code was {} Payment cannot be cancelled. Payment is not in \"Authorized\" -state.", paymentId, response.getResult());
					break;
				case 3:
					log.debug("Cancel request for paymentId: {} result code was {} Transaction for given order_number was not found.", paymentId, response.getResult());
					break;
				case 10:
					log.debug("Cancel request for paymentId: {} result code was {} Maintenance break.", paymentId, response.getResult());
					break;
				default:
					log.debug("Cancel request for paymentId: {} result code was {}", paymentId, response.getResult());
					break;
			}

			return errorMsg + "Please check that api key and private key are correct.";
		}

		throw new IllegalArgumentException("Response was successful. Should never get here if called properly.");
	}
}

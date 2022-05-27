package fi.hel.verkkokauppa.payment.logic.fetcher;

import fi.hel.verkkokauppa.payment.util.CurrencyUtil;
import org.helsinki.vismapay.VismaPayClient;
import org.helsinki.vismapay.model.paymentmethods.PaymentMethod;
import org.helsinki.vismapay.request.paymentmethods.PaymentMethodsRequest;
import org.helsinki.vismapay.response.paymentmethods.PaymentMethodsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
public class PaymentMethodListFetcher {
    
    private Logger log = LoggerFactory.getLogger(PaymentMethodListFetcher.class);

    @Autowired
    private Environment env;

	public PaymentMethod[] getList(String currency) {
		String apiKey = env.getRequiredProperty("payment_api_key");
		String encryptionKey = env.getRequiredProperty("payment_encryption_key");
		String apiVersion = env.getRequiredProperty("payment_methods_api_version");

		VismaPayClient client = new VismaPayClient(apiKey, encryptionKey, apiVersion);

		CompletableFuture<PaymentMethodsResponse> responseCF =
				client.sendRequest(new PaymentMethodsRequest(buildPayloadFor(currency)));

		try {
			PaymentMethodsResponse response = responseCF.get();
			if (response.getResult() == 0) {
				return response.getPaymentMethods();
			} else {
				log.error("payment methods request failed, check application.properties");
				log.debug("Visma PaymentMethodListFetcher error response {}", response);
				throw new RuntimeException(
						"Unable to get the payment methods for the merchant. " +
						"Please check that api key and private key are correct."
				);
			}
		} catch (InterruptedException | ExecutionException e) {
			log.error("exception when getting payment methods", e);
			throw new RuntimeException("Got the following exception: " + e.getMessage());
		}
	}

	private PaymentMethodsRequest.PaymentMethodsPayload buildPayloadFor(String currency) {
		PaymentMethodsRequest.PaymentMethodsPayload payload
				= new PaymentMethodsRequest.PaymentMethodsPayload();
		payload.setCurrency(currency != null ? currency : CurrencyUtil.DEFAULT_CURRENCY);
		return payload;
	}
}

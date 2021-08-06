package fi.hel.verkkokauppa.payment.logic;

import org.helsinki.vismapay.VismaPayClient;
import org.helsinki.vismapay.model.paymentmethods.PaymentMethod;
import org.helsinki.vismapay.request.paymentmethods.PaymentMethodsRequest;
import org.helsinki.vismapay.response.paymentmethods.PaymentMethodsResponse;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
public class PaymentMethodListFetcher {

	public final static String DEFAULT_CURRENCY = "EUR";

	public PaymentMethod[] getList(String currency) {
		// Version is different for payment method API
		VismaPayClient client = new VismaPayClient("api_key", "private_key", "2"); // TODO: replace

		CompletableFuture<PaymentMethodsResponse> responseCF =
				client.sendRequest(new PaymentMethodsRequest(buildPayloadFor(currency)));

		try {
			PaymentMethodsResponse response = responseCF.get();
			if (response.getResult() == 0) {
				return response.getPaymentMethods();
			} else {
				throw new RuntimeException(
						"Unable to get the payment methods for the merchant. " +
						"Please check that api key and private key are correct."
				);
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException("Got the following exception: " + e.getMessage());
		}
	}

	private PaymentMethodsRequest.PaymentMethodsPayload buildPayloadFor(String currency) {
		PaymentMethodsRequest.PaymentMethodsPayload payload
				= new PaymentMethodsRequest.PaymentMethodsPayload();
		payload.setCurrency(currency != null ? currency : DEFAULT_CURRENCY);
		return payload;
	}
}

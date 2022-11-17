package fi.hel.verkkokauppa.payment.paytrail.factory;

import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;
import org.helsinki.paytrail.PaytrailClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaytrailAuthClientFactory {

	@Value("${paytrail.aggregate.merchant.id:}")
	private String aggregateMerchantId;

	@Value("${paytrail.aggregate.merchant.secret:}")
	private String aggregateSecretKey;


	/**
	 * @param customerMerchantId Merchant ID for the item. Required for Shop-in-Shop payments, do not use for normal payments.
	 */
	public PaytrailClient getShopInShopClient(String customerMerchantId) {
		PaytrailClient client = new PaytrailClient(aggregateMerchantId, aggregateSecretKey);
		client.setCustomerMerchantId(customerMerchantId);
		return client;
	}

	/**
	 *
	 * @param paytrailMerchantId Merchant ID used for normal merchant flow
	 * @param paytrailSecretKey Secret key used for normal merchant flow
	 * @return paytrailClient Paytrail client for Paytrail Payment API
	 */
	public PaytrailClient getClient(String paytrailMerchantId, String paytrailSecretKey) {
		return new PaytrailClient(paytrailMerchantId, paytrailSecretKey);
	}
}

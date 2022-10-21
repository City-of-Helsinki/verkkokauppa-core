package fi.hel.verkkokauppa.payment.paytrail.factory;

import org.helsinki.paytrail.PaytrailClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaytrailAuthClientFactory {

	@Value("${paytrail.aggregate.merchant.id:}")
	private String aggregateMerchantId;

	@Value("${paytrail.merchant.secret:}")
	private String secretKey;

	/**
	 * @param customerMerchantId Merchant ID for the item. Required for Shop-in-Shop payments, do not use for normal payments.
	 */
	public PaytrailClient getClient(String customerMerchantId) {
		PaytrailClient client = new PaytrailClient(this.aggregateMerchantId, this.secretKey);
		client.setCustomerMerchantId(customerMerchantId);
		return client;
	}

	public PaytrailClient getClient() {
		PaytrailClient client = new PaytrailClient(this.aggregateMerchantId, this.secretKey);
		return client;
	}
}

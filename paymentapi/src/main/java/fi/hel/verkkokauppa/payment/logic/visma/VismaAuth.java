package fi.hel.verkkokauppa.payment.logic.visma;

import org.helsinki.vismapay.VismaPayClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class VismaAuth {
    
    private Logger log = LoggerFactory.getLogger(VismaAuth.class);

    @Autowired
    private Environment env;

	public VismaPayClient getClient() {
		String apiKey = env.getRequiredProperty("payment_api_key");
		String encryptionKey = env.getRequiredProperty("payment_encryption_key");
		String apiVersion = env.getRequiredProperty("payment_transaction_api_version");

		return new VismaPayClient(apiKey, encryptionKey, apiVersion);
	}
}

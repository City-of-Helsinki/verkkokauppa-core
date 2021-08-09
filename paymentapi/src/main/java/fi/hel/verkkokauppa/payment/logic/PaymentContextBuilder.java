package fi.hel.verkkokauppa.payment.logic;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import fi.hel.verkkokauppa.payment.service.ServiceConfigurationClient;

@Component
public class PaymentContextBuilder {
    
    private Logger log = LoggerFactory.getLogger(PaymentContextBuilder.class);

    @Autowired
    private Environment env;

    @Autowired
    private ServiceConfigurationClient serviceConfigurationClient;


    public PaymentContext buildFor(String namespace) {
        PaymentContext context = new PaymentContext();
        context.setNamespace(namespace);

        // set default values
        context.setDefaultCurrency(env.getRequiredProperty("payment_default_currency"));
        context.setDefaultLanguage(env.getRequiredProperty("payment_default_language"));
        context.setReturnUrl(env.getRequiredProperty("payment_default_return_url"));
        context.setNotifyUrl(env.getRequiredProperty("payment_default_notify_url"));
        context.setMerchantId(Long.valueOf(env.getRequiredProperty("payment_default_submerchant_id")));
        context.setCp(env.getRequiredProperty("payment_default_cp"));

        // fetch namespace specific service configuration from mapping api
        try {
            PaymentContext enrichedContext = enrichWithNamespaceConfiguration(context);
            return enrichedContext;
        } catch (Exception e) {
            log.error("failed to fetch service configuration for namespace: " + namespace, e);
            // TODO by default allowing payments with defaults
        }

        return context;
    }

    private JSONObject getNamespaceConfiguration(String namespace) {
        WebClient client = serviceConfigurationClient.getClient();
        JSONObject namespaceServiceConfiguration = serviceConfigurationClient.getAllServiceConfiguration(client, namespace);
        
        // TODO caching

        return namespaceServiceConfiguration;
    }

    private PaymentContext enrichWithNamespaceConfiguration(PaymentContext context) {
        // refer to ServiceConfigurationKeys at mapping api
        JSONObject namespaceServiceConfiguration = getNamespaceConfiguration(context.getNamespace());

        String returnUrl = (String) namespaceServiceConfiguration.get("payment_return_url");
        if (returnUrl != null) 
            context.setReturnUrl(returnUrl);

        String notifyUrl = (String) namespaceServiceConfiguration.get("payment_notification_url");
        if (notifyUrl != null)
            context.setNotifyUrl(notifyUrl);

        String merchantId = (String) namespaceServiceConfiguration.get("payment_submerchant_id");
        if (merchantId != null)
            context.setMerchantId(Long.valueOf(merchantId));

        String cp = (String) namespaceServiceConfiguration.get("payment_cp");
        if (cp != null)    
            context.setCp(cp);

        return context;
    }

}

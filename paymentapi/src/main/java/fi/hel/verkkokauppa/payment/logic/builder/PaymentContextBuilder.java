package fi.hel.verkkokauppa.payment.logic.builder;

import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.rest.dto.configuration.ServiceConfigurationDto;
import fi.hel.verkkokauppa.payment.logic.context.PaymentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentContextBuilder {
    
    private Logger log = LoggerFactory.getLogger(PaymentContextBuilder.class);

    @Autowired
    private Environment env;

    @Autowired
    private CommonServiceConfigurationClient commonServiceConfigurationClient;


    public PaymentContext buildFor(String namespace) {
        PaymentContext defaultContext = new PaymentContext();
        defaultContext.setNamespace(namespace);

        // set default values
        defaultContext.setDefaultCurrency(env.getRequiredProperty("payment_default_currency"));
        defaultContext.setDefaultLanguage(env.getRequiredProperty("payment_default_language"));
        defaultContext.setReturnUrl(env.getRequiredProperty("payment_default_return_url"));
        defaultContext.setNotifyUrl(env.getRequiredProperty("payment_default_notify_url"));
        defaultContext.setMerchantId(Long.valueOf(env.getRequiredProperty("payment_default_submerchant_id")));
        defaultContext.setCp(env.getRequiredProperty("payment_default_cp"));

        // fetch namespace specific service configuration from mapping api
        PaymentContext namespaceContext = null;
        try {
            namespaceContext = enrichWithNamespaceConfiguration(defaultContext);
        } catch (Exception e) {
            log.error("failed to fetch service configuration for namespace: " + namespace, e);
            // TODO by default allowing payments with defaults
        }

        if (namespaceContext != null) {
            log.debug("using namespace specific service configuration");
            return namespaceContext;
        } else {
            log.debug("using default service configuration");
            return defaultContext;
        }
    }

    private PaymentContext enrichWithNamespaceConfiguration(PaymentContext context) {
        // refer to ServiceConfigurationKeys at mapping api
        List<ServiceConfigurationDto> namespaceServiceConfiguration = commonServiceConfigurationClient.getRestrictedServiceConfigurations(context.getNamespace());

        for (ServiceConfigurationDto configDto : namespaceServiceConfiguration) {
            String key = configDto.getConfigurationKey();
            String value = configDto.getConfigurationValue();
            if (key.equals("payment_return_url") && value != null) {
                context.setReturnUrl(value);
            }

            if (key.equals("payment_notification_url") && value != null) {
                context.setNotifyUrl(value);
            }

            if (key.equals("payment_submerchant_id") && value != null) {
                context.setMerchantId(Long.valueOf(value));
            }

            if (key.equals("payment_cp") && value != null) {
                context.setCp(value);
            }

        }

        return context;
    }

}

package fi.hel.verkkokauppa.payment.logic.builder;

import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.rest.dto.configuration.MerchantDto;
import fi.hel.verkkokauppa.common.util.ConfigurationParseUtil;
import fi.hel.verkkokauppa.payment.logic.context.PaytrailPaymentContext;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import fi.hel.verkkokauppa.payment.service.ServiceConfigurationClient;

@Component
public class PaytrailPaymentContextBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(PaytrailPaymentContextBuilder.class);

    @Autowired
    private Environment env;

    @Autowired
    private ServiceConfigurationClient serviceConfigurationClient;

    @Autowired
    private CommonServiceConfigurationClient commonServiceConfigurationClient;


    public PaytrailPaymentContext buildFor(String namespace, String merchantId) {
        PaytrailPaymentContext defaultContext = new PaytrailPaymentContext();
        defaultContext.setNamespace(namespace);

        // set default values
        defaultContext.setDefaultCurrency(env.getRequiredProperty("payment_default_paytrail_currency"));
        defaultContext.setDefaultLanguage(env.getRequiredProperty("payment_default_paytrail_language"));
        defaultContext.setReturnUrl(env.getRequiredProperty("payment_default_paytrail_return_url"));
        defaultContext.setNotifyUrl(env.getRequiredProperty("payment_default_paytrail_notify_url"));
        defaultContext.setAggregateMerchantId(env.getRequiredProperty("payment_default_paytrail_aggregate_merchant_id"));
        defaultContext.setCp(env.getRequiredProperty("payment_default_paytrail_cp"));

        // fetch namespace and merchant specific service configuration from mapping api
        PaytrailPaymentContext enrichedContext = null;
        try {
            enrichedContext = enrichWithNamespaceConfiguration(defaultContext);
            enrichedContext = enrichWithMerchantConfiguration(enrichedContext, merchantId);
        } catch (Exception e) {
            LOG.error("failed to fetch service configuration for namespace {} and merchant {}: " + namespace, merchantId, e);
            // TODO by default allowing payments with defaults
        }

        if (enrichedContext != null) {
            LOG.debug("using merchant specific service configuration");
            return enrichedContext;
        } else {
            LOG.debug("using default service configuration");
            return defaultContext;
        }
    }

    private JSONObject getNamespaceConfiguration(String namespace) {
        WebClient client = serviceConfigurationClient.getClient();
        JSONObject namespaceServiceConfiguration = serviceConfigurationClient.getAllServiceConfiguration(client, namespace);

        // TODO caching

        return namespaceServiceConfiguration;
    }

    private PaytrailPaymentContext enrichWithNamespaceConfiguration(PaytrailPaymentContext context) {
        JSONObject namespaceServiceConfiguration = getNamespaceConfiguration(context.getNamespace());

        String returnUrl = (String) namespaceServiceConfiguration.get("payment_return_url");
        if (returnUrl != null) {
            context.setReturnUrl(returnUrl);
        }

        String notifyUrl = (String) namespaceServiceConfiguration.get("payment_notification_url");
        if (notifyUrl != null) {
            context.setNotifyUrl(notifyUrl);
        }

        String cp = (String) namespaceServiceConfiguration.get("payment_cp");
        if (cp != null)
            context.setCp(cp);

        return context;
    }

    private PaytrailPaymentContext enrichWithMerchantConfiguration(PaytrailPaymentContext context, String merchantId) {
        MerchantDto merchantConfiguration = commonServiceConfigurationClient.getMerchantModel(merchantId, context.getNamespace());

        if (merchantConfiguration != null) {
            ConfigurationParseUtil.parseConfigurationValueByKey(merchantConfiguration.getConfigurations(), ServiceConfigurationKeys.MERCHANT_SHOP_ID)
                    .ifPresent(config -> context.setShopId(config.getValue()));
        } else {
            LOG.debug("No merchant configurations found for merchant {} in namespace {}", merchantId, context.getNamespace());
        }

        return context;
    }

}

package fi.hel.verkkokauppa.payment.paytrail.context;

import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.rest.dto.configuration.MerchantDto;
import fi.hel.verkkokauppa.common.rest.dto.configuration.ServiceConfigurationDto;
import fi.hel.verkkokauppa.common.util.ConfigurationParseUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class PaytrailPaymentContextBuilder {

    @Autowired
    private Environment env;

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
            log.error("failed to fetch service configuration for namespace {} and merchant {}: " + namespace, merchantId, e);
        }

        if (enrichedContext != null) {
            log.debug("using merchant specific service configuration");
            return enrichedContext;
        } else {
            log.debug("using default service configuration");
            return defaultContext;
        }
    }

    private PaytrailPaymentContext enrichWithNamespaceConfiguration(PaytrailPaymentContext context) {
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

            if (key.equals("payment_cp") && value != null) {
                context.setCp(value);
            }
        }
        return context;
    }

    private PaytrailPaymentContext enrichWithMerchantConfiguration(PaytrailPaymentContext context, String merchantId) {
        MerchantDto merchantConfiguration = commonServiceConfigurationClient.getMerchantModel(merchantId, context.getNamespace());

        if (merchantConfiguration != null) {
            ConfigurationParseUtil.parseConfigurationValueByKey(merchantConfiguration.getConfigurations(), ServiceConfigurationKeys.MERCHANT_SHOP_ID)
                    .ifPresent(config -> context.setShopId(config.getValue()));
        } else {
            log.debug("No merchant configurations found for merchant {} in namespace {}", merchantId, context.getNamespace());
        }

        return context;
    }

}

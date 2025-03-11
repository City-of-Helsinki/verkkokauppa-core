package fi.hel.verkkokauppa.payment.paytrail.context;

import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.rest.dto.configuration.MerchantDto;
import fi.hel.verkkokauppa.common.rest.dto.configuration.ServiceConfigurationDto;
import fi.hel.verkkokauppa.common.util.ConfigurationParseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class PaytrailPaymentContextBuilder {

    @Autowired
    private Environment env;

    @Autowired
    private CommonServiceConfigurationClient commonServiceConfigurationClient;

    /**
     * Build payment context for paytrail with shop-in-shop or normal merchant flow.
     * @param namespace Namespace
     * @param merchantId Internal merchant id
     * @param useShopInShop flag determines whether to create PaytrailPaymentContext for shop-in-shop or normal merchant flow
     * @return
     */
    public PaytrailPaymentContext buildFor(String namespace, String merchantId, boolean useShopInShop) throws CommonApiException {
        PaytrailPaymentContext defaultContext = new PaytrailPaymentContext();
        defaultContext.setNamespace(namespace);
        defaultContext.setInternalMerchantId(merchantId);
        defaultContext.setUseShopInShop(useShopInShop);

        // set default values
        defaultContext.setDefaultCurrency(env.getRequiredProperty("payment_default_paytrail_currency"));
        defaultContext.setDefaultLanguage(env.getRequiredProperty("payment_default_paytrail_language"));
        defaultContext.setReturnUrl(env.getRequiredProperty("payment_default_paytrail_return_url"));
        defaultContext.setNotifyUrl(env.getRequiredProperty("payment_default_paytrail_notify_url"));
        defaultContext.setCp(env.getRequiredProperty("payment_default_paytrail_cp"));

        defaultContext.setCardRedirectSuccessUrl(env.getRequiredProperty("paytrail_card_redirect_success_url"));
        defaultContext.setCardRedirectCancelUrl(env.getRequiredProperty("paytrail_card_redirect_cancel_url"));
        defaultContext.setCardCallbackSuccessUrl(env.getRequiredProperty("paytrail_card_callback_success_url"));
        defaultContext.setCardCallbackCancelUrl(env.getRequiredProperty("paytrail_card_callback_cancel_url"));

        defaultContext.setUpdateCardRedirectSuccessUrl(env.getRequiredProperty("paytrail_update_card_redirect_success_url"));
        defaultContext.setUpdateCardRedirectCancelUrl(env.getRequiredProperty("paytrail_update_card_redirect_cancel_url"));
        defaultContext.setUpdateCardCallbackSuccessUrl(env.getRequiredProperty("paytrail_update_card_callback_success_url"));
        defaultContext.setUpdateCardCallbackCancelUrl(env.getRequiredProperty("paytrail_update_card_callback_cancel_url"));

        defaultContext.setPaymentRedirectSuccessUrl(env.getRequiredProperty("paytrail_payment_return_success_url"));
        defaultContext.setPaymentRedirectCancelUrl(env.getRequiredProperty("paytrail_payment_return_cancel_url"));
        defaultContext.setPaymentCallbackSuccessUrl(env.getRequiredProperty("paytrail_payment_notify_success_url"));
        defaultContext.setPaymentCallbackCancelUrl(env.getRequiredProperty("paytrail_payment_notify_cancel_url"));

        // fetch namespace and merchant specific service configuration from mapping api
        try {
            defaultContext = enrichWithNamespaceConfiguration(defaultContext);
            defaultContext = enrichWithMerchantConfiguration(defaultContext, merchantId);
        } catch (CommonApiException e) {
            log.error("failed to fetch service configuration for namespace {} and merchant {}: " + namespace, merchantId, e);
            throw e;
        }

        return defaultContext;
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

            if (key.equals("refund_success_notification_url") && value != null) {
                context.setRefundCallbackSuccessUrl(value);
            }
            if (key.equals("refund_cancel_notification_url") && value != null) {
                context.setRefundCallbackCancelUrl(value);
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
            if (context.isUseShopInShop()) {
                // Paytrail Merchant shop ID (for shop-in-shop flow)
                ConfigurationParseUtil.parseConfigurationValueByKey(merchantConfiguration.getConfigurations(), ServiceConfigurationKeys.MERCHANT_SHOP_ID)
                        .ifPresent(config -> context.setShopId(config.getValue()));
            } else {
                // Merchant specific Paytrail merchant ID and secret key if merchant ID is present
                ConfigurationParseUtil.parseConfigurationValueByKey(merchantConfiguration.getConfigurations(), ServiceConfigurationKeys.MERCHANT_PAYTRAIL_MERCHANT_ID)
                        .ifPresent(config -> {
                            String merchantSecretkey = commonServiceConfigurationClient.getMerchantPaytrailSecretKey(merchantId);
                            context.setPaytrailSecretKey(merchantSecretkey);
                            context.setPaytrailMerchantId(config.getValue());
                        });

                if (context.getPaytrailSecretKey() == null) {
                    throw new CommonApiException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            new Error("failed-to-get-merchant-paytrail-secret-key", "failed to get paytrail secret key, merchantId: " + merchantId)
                    );
                }
                if (context.getPaytrailMerchantId() == null) {
                    throw new CommonApiException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            new Error("failed-to-get-paytrail-merchant-id", "failed to get paytrail merchant id, merchantId: " + merchantId + ", namespace: " + context.getNamespace())
                    );
                }
            }
        } else {
            log.debug("No merchant configurations found for merchant {} in namespace {}", merchantId, context.getNamespace());
        }

        return context;
    }

}

package fi.hel.verkkokauppa.common.configuration;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ServiceConfigurationKeys {

    //
    // Payment related configuration keys, based on KYV-60 Jira task
    //
    // Stays in ServiceConfigurations START
    public static String NAMESPACE_API_ACCESS_TOKEN = "namespaceApiAccessToken";
    public static String PAYMENT_API_VERSION = "payment_api_version";
    public static String PAYMENT_API_KEY = "payment_api_key";
    public static String PAYMENT_ENCRYPTION_KEY = "payment_encryption_key";
    public static String PAYMENT_CURRENCY = "payment_currency";
    public static String PAYMENT_TYPE = "payment_type";
    public static String PAYMENT_REGISTER_CARD_TOKEN = "payment_register_card_token";
    public static String PAYMENT_RETURN_URL = "payment_return_url";
    public static String PAYMENT_NOTIFICATION_URL = "payment_notification_url";
    public static String PAYMENT_LANGUAGE = "payment_language";
    public static String PAYMENT_SUBMERCHANT_ID = "payment_submerchant_id";
    public static String PAYMENT_CP = "payment_cp";
    // Stays in ServiceConfigurations END


    //
    // Merchant info related keys from KYV-182 (values can be fetched from serviceconfiguration,merchant model)
    //
    public static String MERCHANT_NAME = "merchantName";
    public static String MERCHANT_STREET = "merchantStreet";
    public static String MERCHANT_ZIP = "merchantZip";
    public static String MERCHANT_CITY = "merchantCity";
    public static String MERCHANT_EMAIL = "merchantEmail";
    public static String MERCHANT_PHONE = "merchantPhone";
    public static String MERCHANT_URL = "merchantUrl";
    // Paytrail specific merchant keys
    public static String MERCHANT_SHOP_ID = "merchantShopId";
    public static String MERCHANT_PAYTRAIL_MERCHANT_ID = "merchantPaytrailMerchantId";
    public static String MERCHANT_PAYTRAIL_SECRET = "merchantPaytrailSecret";


    // NamespaceModel keys START [KYV-605] (values can be fetched from serviceconfiguration,namespace model)
    // Subscription price end point KYV-462
    public static String SUBSCRIPTION_PRICE_URL = "subscriptionPriceUrl";
    public static String MERCHANT_TERMS_OF_SERVICE_URL = "merchantTermsOfServiceUrl";
    public static String ORDER_CANCEL_REDIRECT_URL = "orderCancelRedirectUrl";
    public static String ORDER_SUCCESS_REDIRECT_URL = "orderSuccessRedirectUrl";
    public static String REFUND_SUCCESS_REDIRECT_URL = "refundSuccessRedirectUrl";
    // Order right of purchase [KYV-233]
    public static String ORDER_RIGHT_OF_PURCHASE_IS_ACTIVE = "orderRightOfPurchaseIsActive";
    public static String ORDER_RIGHT_OF_PURCHASE_URL = "orderRightOfPurchaseUrl";
    // Refund webhook [KYV-559]
    public static String MERCHANT_REFUND_WEBHOOK_URL = "merchantRefundWebhookUrl";
    // Merchant webhooks [KYV-350]
    public static String MERCHANT_PAYMENT_WEBHOOK_URL = "merchantPaymentWebhookUrl";
    // Order webhook [KYV-409]
    public static String MERCHANT_ORDER_WEBHOOK_URL = "merchantOrderWebhookUrl";
    // Subscription webhook [KYV-405]
    public static String MERCHANT_SUBSCRIPTION_WEBHOOK_URL = "merchantSubscriptionWebhookUrl";
    // NamespaceModel keys END [KYV-605]

    public static List<String> getAllConfigurationKeys() {
        List<String> knownKeys = new ArrayList<>();
        knownKeys.addAll(new ArrayList<>(getNamespaceAndMerchantConfigurationKeys()));
        knownKeys.addAll(new ArrayList<>(getPlatformKeys()));
        return knownKeys.stream()
                .distinct() // Removes duplicates
                .sorted()
                .collect(Collectors.toList());
    }


    public static List<String> getNamespaceAndMerchantConfigurationKeys() {
        List<String> namespaceKeys = getNamespaceKeys();
        List<String> merchantKeys = getMerchantKeys();
        namespaceKeys.addAll(merchantKeys);
        return namespaceKeys
                .stream()
                .distinct() // Removes duplicates
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Platform keys comes from serviceconfiguration or namespace model
     */
    public static List<String> getPlatformKeys() {
        return Stream.of(
                PAYMENT_API_VERSION,
                PAYMENT_API_KEY,
                PAYMENT_ENCRYPTION_KEY,
                PAYMENT_CURRENCY,
                PAYMENT_TYPE,
                PAYMENT_REGISTER_CARD_TOKEN,
                PAYMENT_RETURN_URL,
                PAYMENT_NOTIFICATION_URL,
                PAYMENT_SUBMERCHANT_ID,
                PAYMENT_LANGUAGE,
                PAYMENT_CP,
                NAMESPACE_API_ACCESS_TOKEN
                ).sorted().collect(Collectors.toList());
    }

    public static List<String> getNamespaceKeys() {
        List<String> namespaceKeys = Stream.of(
                MERCHANT_PAYMENT_WEBHOOK_URL,      // must not be overwritten by (merchant)
                MERCHANT_ORDER_WEBHOOK_URL,        // must not be overwritten by (merchant)
                MERCHANT_SUBSCRIPTION_WEBHOOK_URL, // must not be overwritten by (merchant)
                MERCHANT_REFUND_WEBHOOK_URL,       // must not be overwritten by (merchant)
                ORDER_CANCEL_REDIRECT_URL,         // must not be overwritten by (merchant)
                ORDER_SUCCESS_REDIRECT_URL,        // must not be overwritten by (merchant)
                REFUND_SUCCESS_REDIRECT_URL        // must not be overwritten by (merchant)
        ).collect(Collectors.toList());

        namespaceKeys.addAll(getOverridableMerchantKeys());

        return namespaceKeys
                .stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public static List<String> getMerchantKeys() {

        List<String> overridableMerchantKeys = getOverridableMerchantKeys();
        List<String> merchantKeys = Stream.of(
                MERCHANT_NAME,
                MERCHANT_STREET,
                MERCHANT_ZIP,
                MERCHANT_CITY,
                MERCHANT_EMAIL,
                MERCHANT_PHONE,
                MERCHANT_URL,
                MERCHANT_SHOP_ID,
                MERCHANT_PAYTRAIL_MERCHANT_ID
        ).sorted().collect(Collectors.toList());

        merchantKeys.addAll(overridableMerchantKeys);
        return merchantKeys
                .stream()
                .distinct() // Removes duplicates
                .sorted()
                .collect(Collectors.toList());
    }

    public static List<String> getOverridableMerchantKeys() {
        return Stream.of(
                MERCHANT_TERMS_OF_SERVICE_URL,     // can be overwritten by (merchant)
                ORDER_RIGHT_OF_PURCHASE_IS_ACTIVE, // can be overwritten by (merchant)
                ORDER_RIGHT_OF_PURCHASE_URL,       // can be overwritten by (merchant)
                SUBSCRIPTION_PRICE_URL            // can be overwritten by (merchant)
        ).sorted().collect(Collectors.toList());
    }

    public static boolean isRestrictedConfigurationKey(String key) {
        return getPlatformKeys().contains(key);
    }

}

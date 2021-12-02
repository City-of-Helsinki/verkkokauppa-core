package fi.hel.verkkokauppa.common.configuration;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class ServiceConfigurationKeys {

    //
    // Payment related configuration keys, based on KYV-60 Jira task
    //
    public static String PAYMENT_API_VERSION = "payment_api_version";
    public static String PAYMENT_API_KEY = "payment_api_key";
    public static String PAYMENT_ENCRYPTION_KEY = "payment_encryption_key";
    // order id
    // order total sum in cents
    public static String PAYMENT_CURRENCY = "payment_currency";
    // authentication code HMAC checksum
    public static String PAYMENT_TYPE = "payment_type";
    public static String PAYMENT_REGISTER_CARD_TOKEN = "payment_register_card_token";
    public static String PAYMENT_RETURN_URL = "payment_return_url";
    public static String PAYMENT_NOTIFICATION_URL = "payment_notification_url";
    public static String PAYMENT_LANGUAGE = "payment_language";
    // order language
    // selected payment method
    // order customer name
    // order customer email
    public static String PAYMENT_SUBMERCHANT_ID = "payment_submerchant_id";
    public static String PAYMENT_CP = "payment_cp";

    //
    // Merchant info related keys from KYV-182
    //
    public static String MERCHANT_NAME = "merchantName";
    public static String MERCHANT_STREET = "merchantStreet";
    public static String MERCHANT_ZIP = "merchantZip";
    public static String MERCHANT_CITY = "merchantCity";
    public static String MERCHANT_EMAIL = "merchantEmail";
    public static String MERCHANT_PHONE = "merchantPhone";
    public static String MERCHANT_URL = "merchantUrl";
    public static String MERCHANT_TERMS_OF_SERVICE_URL = "merchantTermsOfServiceUrl";
    // Merchant webhooks [KYV-350]
    public static String MERCHANT_PAYMENT_WEBHOOK_URL = "merchantPaymentWebhookUrl";
    // Order webhook [KYV-409]
    public static String MERCHANT_ORDER_WEBHOOK_URL = "merchantOrderWebhookUrl";
    // Subscription webhook [KYV-405]
    public static String MERCHANT_SUBSCRIPTION_WEBHOOK_URL = "merchantSubscriptionWebhookUrl";

    public static String NAMESPACE_API_ACCESS_TOKEN = "namespaceApiAccessToken";

    // Order right of purchase [KYV-233]
    public static String ORDER_RIGHT_OF_PURCHASE_IS_ACTIVE = "orderRightOfPurchaseIsActive";
    public static String ORDER_RIGHT_OF_PURCHASE_URL = "orderRightOfPurchaseUrl";


    public static List<String> getAllConfigurationKeys() {
        List<String> knownKeys = getUnrestrictedConfigurationKeys();
        knownKeys.addAll(getRestrictedConfigurationKeys());
        return knownKeys;
    }

    public static List<String> getUnrestrictedConfigurationKeys() {
        return Arrays.asList(MERCHANT_NAME, MERCHANT_STREET, MERCHANT_ZIP, MERCHANT_CITY, MERCHANT_EMAIL,
                MERCHANT_PHONE, MERCHANT_URL, MERCHANT_TERMS_OF_SERVICE_URL,
                MERCHANT_PAYMENT_WEBHOOK_URL, ORDER_RIGHT_OF_PURCHASE_IS_ACTIVE, ORDER_RIGHT_OF_PURCHASE_URL, MERCHANT_ORDER_WEBHOOK_URL, MERCHANT_SUBSCRIPTION_WEBHOOK_URL);
    }

    public static List<String> getRestrictedConfigurationKeys() {
        return Arrays.asList(PAYMENT_API_VERSION, PAYMENT_API_KEY, PAYMENT_CURRENCY, PAYMENT_TYPE, PAYMENT_REGISTER_CARD_TOKEN,
                PAYMENT_RETURN_URL, PAYMENT_NOTIFICATION_URL, PAYMENT_LANGUAGE, PAYMENT_SUBMERCHANT_ID, PAYMENT_CP
        );
    }

    public static boolean isRestrictedConfigurationKey(String key) {
        return getRestrictedConfigurationKeys().contains(key);
    }

}

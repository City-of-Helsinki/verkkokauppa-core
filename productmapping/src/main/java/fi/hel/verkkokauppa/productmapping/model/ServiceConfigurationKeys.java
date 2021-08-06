package fi.hel.verkkokauppa.productmapping.model;

import java.util.Arrays;
import java.util.List;

public class ServiceConfigurationKeys {
    
    public static String TERMS_OF_USE_URL = "terms_of_use_url";
    public static String TERMS_OF_USE_EMBEDDABLE_CONTENT = "terms_of_use_embeddable_content";
    public static String ORDER_CREATED_REDIRECT_URL = "order_created_redirect_url";

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


    public static List<String> getRestrictedConfigurationKeys() {
        return Arrays.asList(new String[]{
            PAYMENT_API_VERSION, PAYMENT_API_KEY, PAYMENT_CURRENCY, PAYMENT_TYPE, PAYMENT_REGISTER_CARD_TOKEN, 
            PAYMENT_RETURN_URL, PAYMENT_NOTIFICATION_URL, PAYMENT_LANGUAGE, PAYMENT_SUBMERCHANT_ID
        });
    }

    public static boolean isRestrictedConfigurationKey(String key) {
        return getRestrictedConfigurationKeys().contains(key);
    }

}

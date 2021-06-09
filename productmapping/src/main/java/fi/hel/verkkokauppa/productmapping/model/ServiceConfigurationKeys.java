package fi.hel.verkkokauppa.productmapping.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServiceConfigurationKeys {
    
    public static String TERMS_OF_USE_URL = "terms_of_use_url";
    public static String TERMS_OF_USE_EMBEDDABLE_CONTENT = "terms_of_use_embeddable_content";
    public static String ORDER_CREATED_REDIRECT_URL = "order_created_redirect_url";

    public static String PAYMENT_MERCHANT_ID = "payment_merchant_id";
    public static String PAYMENT_SUCCESS_REDIRECT_URL = "payment_success_redirect_url";
    public static String PAYMENT_FAILURE_REDIRECT_URL = "payment_failure_redirect_url";


    public static List<String> getRestrictedConfigurationKeys() {
        return Arrays.asList(new String[]{PAYMENT_MERCHANT_ID, PAYMENT_SUCCESS_REDIRECT_URL, PAYMENT_FAILURE_REDIRECT_URL});
    }

    public static boolean isRestrictedConfigurationKey(String key) {
        return getRestrictedConfigurationKeys().contains(key);
    }

}

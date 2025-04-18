package fi.hel.verkkokauppa.payment.paytrail.context;

import lombok.Data;

@Data
public class PaytrailPaymentContext {

    private String namespace;

    /* merchantId -> internalMerchantId. This is needed to get merchant specific configurations for normal merchant flow */
    private String internalMerchantId;
    private String returnUrl;
    private String notifyUrl;
    private String cp;
    private String defaultCurrency;
    private String defaultLanguage;
    private boolean useShopInShop;

    /* Paytrail normal merchant flow */
    private String paytrailMerchantId;
    private String paytrailSecretKey;

    /* Paytrail Shop-in-Shop merchant ID */
    private String shopId;

    private String cardRedirectSuccessUrl;
    private String cardRedirectCancelUrl;
    private String cardCallbackSuccessUrl;
    private String cardCallbackCancelUrl;
    private String updateCardRedirectSuccessUrl;
    private String updateCardRedirectCancelUrl;
    private String updateCardCallbackSuccessUrl;
    private String updateCardCallbackCancelUrl;
    private String paymentRedirectSuccessUrl;
    private String paymentRedirectCancelUrl;
    private String paymentCallbackSuccessUrl;
    private String paymentCallbackCancelUrl;
    private String refundCallbackSuccessUrl;
    private String refundCallbackCancelUrl;
}

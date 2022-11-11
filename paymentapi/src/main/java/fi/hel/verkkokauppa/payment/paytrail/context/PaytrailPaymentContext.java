package fi.hel.verkkokauppa.payment.paytrail.context;

import lombok.Data;

@Data
public class PaytrailPaymentContext {

    private String namespace;
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
}

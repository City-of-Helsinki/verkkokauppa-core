package fi.hel.verkkokauppa.payment.logic.context;

import lombok.Data;

@Data
public class PaytrailPaymentContext {

    private String namespace;
    private String returnUrl;
    private String notifyUrl;
    private String aggregateMerchantId;
    private String shopId;
    private String cp;
    private String defaultCurrency;
    private String defaultLanguage;
}

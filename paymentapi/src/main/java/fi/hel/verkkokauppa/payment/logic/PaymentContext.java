package fi.hel.verkkokauppa.payment.logic;

public class PaymentContext {
    private String namespace;
    private String returnUrl;
    private String notifyUrl;
    private Long merchantId;
    private String cp;
    private String defaultCurrency;
    private String defaultLanguage;


    public String getReturnUrl() {
        return returnUrl;
    }
    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }
    public String getNotifyUrl() {
        return notifyUrl;
    }
    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }
    public Long getMerchantId() {
        return merchantId;
    }
    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }
    public String getCp() {
        return cp;
    }
    public void setCp(String cp) {
        this.cp = cp;
    }
    public String getNamespace() {
        return namespace;
    }
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
    public String getDefaultCurrency() {
        return defaultCurrency;
    }
    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }
    public String getDefaultLanguage() {
        return defaultLanguage;
    }
    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }
 
}

package fi.hel.verkkokauppa.mockproductmanagement.api;

public class MockPrice {
    String id;
    String namespace;
    String productId;
    String netValue;
    String vatPercentage;
    String vatValue;
    String grossValue;

    public MockPrice() {}

    public MockPrice(String id, String namespace, String productId, String netValue, String vatPercentage,
            String vatValue, String grossValue) {
        this.id = id;
        this.namespace = namespace;
        this.productId = productId;
        this.netValue = netValue;
        this.vatPercentage = vatPercentage;
        this.vatValue = vatValue;
        this.grossValue = grossValue;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getNetValue() {
        return netValue;
    }

    public void setNetValue(String netValue) {
        this.netValue = netValue;
    }

    public String getVatValue() {
        return vatValue;
    }

    public void setVatValue(String vatValue) {
        this.vatValue = vatValue;
    }

    public String getVatPercentage() {
        return vatPercentage;
    }

    public void setVatPercentage(String vatPercentage) {
        this.vatPercentage = vatPercentage;
    }

    public String getGrossValue() {
        return grossValue;
    }

    public void setGrossValue(String grossValue) {
        this.grossValue = grossValue;
    }

}

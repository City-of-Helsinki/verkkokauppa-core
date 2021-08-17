package fi.hel.verkkokauppa.mockproductmanagement.api;

public class MockRightOfPurchase {
    private String id;
    private String namespace;
    private String productId;
    private boolean rightOfPurchase;

    public MockRightOfPurchase() {        
    }
    
    public MockRightOfPurchase(String id, String namespace, String productId, boolean rightOfPurchase) {
        this.id = id;
        this.namespace = namespace;
        this.productId = productId;
        this.rightOfPurchase = rightOfPurchase;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public boolean isRightOfPurchase() {
        return rightOfPurchase;
    }

    public void setRightOfPurchase(boolean rightOfPurchase) {
        this.rightOfPurchase = rightOfPurchase;
    }

}

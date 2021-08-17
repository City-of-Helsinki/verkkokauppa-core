package fi.hel.verkkokauppa.mockproductmanagement.api;

public class MockAvailability {
    private String id;
    private String namespace;
    private String productId;
    private boolean available;
    
    public MockAvailability() {
    }

    public MockAvailability(String id, String namespace, String productId, boolean available) {
        this.id = id;
        this.namespace = namespace;
        this.productId = productId;
        this.available = available;
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

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

}

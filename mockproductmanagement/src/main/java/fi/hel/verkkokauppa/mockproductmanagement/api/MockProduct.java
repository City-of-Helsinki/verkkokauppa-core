package fi.hel.verkkokauppa.mockproductmanagement.api;

public class MockProduct {
    String id;
    String name;
    String description;
    String namespace;

    public MockProduct() {}

    public MockProduct(String id, String name, String description, String namespace) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.namespace = namespace;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
    
}

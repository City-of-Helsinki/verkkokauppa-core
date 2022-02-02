package fi.hel.verkkokauppa.order.interfaces;

public interface Product {
    String getProductName();

    String getProductId();

    void setProductName(String productName);

    void setProductId(String productId);

    String getProductLabel();

    void setProductLabel(String productLabel);

    String getProductDescription();

    void setProductDescription(String productDescription);
}

package fi.hel.verkkokauppa.price.model;

import com.fasterxml.jackson.annotation.JsonRawValue;

import org.json.JSONObject;

public class Price {
    String productId;
    String price;
    @JsonRawValue
    JSONObject original;

    public Price() {}

    public Price(String productId, String price, JSONObject original) {
        this.productId = productId;
        this.price = price.replace(",", ".");
        this.original = original;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public JSONObject getOriginal() {
        return original;
    }

    public void setOriginal(JSONObject original) {
        this.original = original;
    }
    
}

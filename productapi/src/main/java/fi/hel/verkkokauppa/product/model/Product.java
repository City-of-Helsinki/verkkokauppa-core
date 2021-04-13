package fi.hel.verkkokauppa.product.model;

import com.fasterxml.jackson.annotation.JsonRawValue;

import org.json.JSONObject;

public class Product {
    String id;
    String name;
    @JsonRawValue
    JSONObject mapping;
    @JsonRawValue
    JSONObject original;

    public Product() {
    }

    public Product(String id, String name, JSONObject mapping, JSONObject original) {
        this.id = id;
        this.name = name;
        this.mapping = mapping;
        this.original = original;
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
    public JSONObject getMapping() {
        return mapping;
    }
    public void setMapping(JSONObject mapping) {
        this.mapping = mapping;
    }
    public JSONObject getOriginal() {
        return original;
    }
    public void setOriginal(JSONObject original) {
        this.original = original;
    }
    
}

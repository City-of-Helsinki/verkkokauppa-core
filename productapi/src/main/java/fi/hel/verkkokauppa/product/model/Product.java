package fi.hel.verkkokauppa.product.model;

import com.fasterxml.jackson.annotation.JsonRawValue;

import org.json.JSONObject;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "products")
public class Product {
    @Id
    String id;
    @Field(type = FieldType.Text)    
    String name;
    @Field(type = FieldType.Text)
    @JsonRawValue
    JSONObject mapping;
    @Field(type = FieldType.Text)
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

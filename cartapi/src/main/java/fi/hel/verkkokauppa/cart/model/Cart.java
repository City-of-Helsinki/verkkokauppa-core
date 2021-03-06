package fi.hel.verkkokauppa.cart.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "carts")
public class Cart {
    @Id
    String cartId;
    @Field(type = FieldType.Keyword)
    String namespace;
    @Field(type = FieldType.Keyword)
    String user;
    @Field(type = FieldType.Text)
    String createdAt;

    public Cart() {}

    public Cart(String cartId, String namespace, String user, String createdAt) {
        this.cartId = cartId;
        this.namespace = namespace;
        this.user = user;
        this.createdAt = createdAt;
    }

    public String getCartId() {
        return cartId;
    }

    public void setCartId(String cartId) {
        this.cartId = cartId;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
}

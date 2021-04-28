package fi.hel.verkkokauppa.cart.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "cartitems")
public class CartItem {
    @Id
    String cartItemId;
    @Field(type = FieldType.Keyword)
    String cartId;
    @Field(type = FieldType.Keyword)
    String productId;
    @Field(type = FieldType.Text)
    Integer quantity;
    @Field(type = FieldType.Text)
    String unit;

    // TODO original product response (not persistent)
    // TODO original price response (not persistent)


    public CartItem() {}

    public CartItem(String cartItemId, String cartId, String productId, Integer quantity, String unit) {
        this.cartItemId = cartItemId;
        this.cartId = cartId;
        this.productId = productId;
        this.quantity = quantity;
        this.unit = unit;
    }

    public String getCartItemId() {
        return cartItemId;
    }

    public void setCartItemId(String cartItemId) {
        this.cartItemId = cartItemId;
    }

    public String getCartId() {
        return cartId;
    }

    public void setCartId(String cartId) {
        this.cartId = cartId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
    
}

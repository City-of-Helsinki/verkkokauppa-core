package fi.hel.verkkokauppa.cart.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.keyvalue.annotation.KeySpace;

@KeySpace("carts")
public class Cart {
    @Id
    String cartId;
    String namespace;
    String user;

    public Cart() {}

    public Cart(String cartId, String namespace, String user) {
        this.cartId = cartId;
        this.namespace = namespace;
        this.user = user;
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
    
}

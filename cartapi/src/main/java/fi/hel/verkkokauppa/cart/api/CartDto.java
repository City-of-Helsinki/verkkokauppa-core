package fi.hel.verkkokauppa.cart.api;

import java.util.List;

import fi.hel.verkkokauppa.cart.model.Cart;
import fi.hel.verkkokauppa.cart.model.CartItem;

/**
 * non-persistent cart representation with both persisted and calculated data
 */
public class CartDto {
    Cart cart;
    List<CartItem> items;

    // TODO original calculated cart price response (no persistence for price data)


    public CartDto() {}

    public CartDto(Cart cart, List<CartItem> items) {
        this.cart = cart;
        this.items = items;
    }

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }
    
}

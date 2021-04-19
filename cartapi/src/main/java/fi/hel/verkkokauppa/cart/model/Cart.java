package fi.hel.verkkokauppa.cart.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.keyvalue.annotation.KeySpace;

@KeySpace("carts")
public class Cart {
    @Id
    String cartId;

}

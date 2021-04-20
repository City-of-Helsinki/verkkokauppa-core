package fi.hel.verkkokauppa.cart.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.hel.verkkokauppa.cart.model.Cart;
import fi.hel.verkkokauppa.utils.UUIDGenerator;

@Component
public class CartService {
        
    private Logger log = LoggerFactory.getLogger(CartService.class);

    @Autowired
    private CartRepository cartRepository;


    public Cart createByParams(String namespace, String user) {
        String cartId = UUIDGenerator.generateType3UUIDString(namespace, user);
        Cart cart = new Cart(cartId, namespace, user);
        cartRepository.save(cart);
        log.debug("created new cart, cartId: " + cartId);

        return cart;
    }

}


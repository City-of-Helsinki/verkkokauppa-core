package fi.hel.verkkokauppa.cart.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

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
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String createdAt = LocalDate.now().format(formatter);

        Cart cart = new Cart(cartId, namespace, user, createdAt);
        cartRepository.save(cart);
        log.debug("created new cart, cartId: " + cartId);

        return cart;
    }

    public Cart findById(String cartId) {
        Optional<Cart> mapping = cartRepository.findById(cartId);
        
        if (mapping.isPresent())
            return mapping.get();

        log.debug("cart not found, cartId: " + cartId);
        return null;
    }

    public Cart findByNames(String namespace, String user) {
        List<Cart> matchingCarts = cartRepository.findByNamespaceAndUser(namespace, user);

        if (matchingCarts.size() > 0)
            return matchingCarts.get(0);

        log.debug("cart not found, namespace: " + namespace + " user: " + user);
        return null;
    }

}


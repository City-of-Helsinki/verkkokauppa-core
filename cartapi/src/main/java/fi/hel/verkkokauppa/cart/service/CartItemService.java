package fi.hel.verkkokauppa.cart.service;

import java.util.List;
import java.util.Optional;

import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.hel.verkkokauppa.cart.model.CartItem;


@Component
public class CartItemService {
            
    private Logger log = LoggerFactory.getLogger(CartService.class);

    @Autowired
    private CartItemRepository cartItemRepository;


    public void addItem(String cartId, String productId, Integer quantity) {
        String cartItemId = UUIDGenerator.generateType3UUIDString(cartId, productId);

        CartItem cartItem = findById(cartItemId);
        if (cartItem != null) {
            Integer newQuantity = cartItem.getQuantity() + quantity;
            cartItem.setQuantity(newQuantity);
            cartItemRepository.save(cartItem);
            log.debug("increased cartItem quantity, cartItemId: " + cartItemId);
        } else {
            cartItem = new CartItem(cartItemId, cartId, productId, quantity, "pcs");
            cartItemRepository.save(cartItem);
            log.debug("created new cartItem, cartItemId: " + cartItemId);
        }
    }

    public void removeItem(String cartId, String productId, Integer quantity) {
        String cartItemId = UUIDGenerator.generateType3UUIDString(cartId, productId);

        CartItem cartItem = findById(cartItemId);
        final boolean isQuantitySpecified = quantity > 0;

        if (isQuantitySpecified) {
            final Integer newQuantity = cartItem.getQuantity() - quantity;
            cartItem.setQuantity(newQuantity);
            cartItemRepository.save(cartItem);    
            log.debug("reduced cartItem quantity, cartItemId: " + cartItemId);                
        } else {
            cartItemRepository.deleteById(cartItemId);
            log.debug("deleted cartItem, cartItemId: " + cartItemId);                
        }
    }

    public void editItemQuantity(String cartId, String productId, Integer quantity) {
        String cartItemId = UUIDGenerator.generateType3UUIDString(cartId, productId);
        CartItem cartItem = findById(cartItemId);
        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);
        log.debug("edited cartItem quantity, cartItemId: " + cartItemId);
    }

    public CartItem findById(String cartItemId) {
        Optional<CartItem> mapping = cartItemRepository.findById(cartItemId);
        
        if (mapping.isPresent())
            return mapping.get();

        log.debug("cartItem not found, cartItemId: " + cartItemId);
        return null;
    }

    public List<CartItem> findByCartId(String cartId) {
        List<CartItem> cartItems = cartItemRepository.findByCartId(cartId);

        if (cartItems.size() > 0)
            return cartItems;

        log.debug("cartItems not found, cartId: " + cartId);
        return null;
    }

    public void removeAllByCartId(String cartId) {
        List<CartItem> cartItems = findByCartId(cartId);
        cartItems.forEach(item -> cartItemRepository.deleteById(item.getCartItemId()));
        log.debug("cartItems removed, cartId: " + cartId);
    }

}

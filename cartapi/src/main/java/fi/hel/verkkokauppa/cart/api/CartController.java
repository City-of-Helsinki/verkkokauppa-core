package fi.hel.verkkokauppa.cart.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fi.hel.verkkokauppa.cart.model.Cart;
import fi.hel.verkkokauppa.cart.model.CartItem;
import fi.hel.verkkokauppa.cart.service.CartItemService;
import fi.hel.verkkokauppa.cart.service.CartService;

@RestController
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartItemService cartItemService;


    @GetMapping("/cart/create")
	public Cart createCart(@RequestParam(value = "namespace") String namespace, 
            @RequestParam(value = "user") String user) {
		return cartService.createByParams(namespace, user);
	}

    @GetMapping("/cart/get")
	public Cart getCart(@RequestParam(value = "cartId") String cartId) {
		return cartService.findById(cartId);
	}

    @GetMapping("/cart/getByNames")
	public Cart getCartByNames(@RequestParam(value = "namespace") String namespace, 
            @RequestParam(value = "user") String user) {
		return cartService.findByNamespaceAndUser(namespace, user);
	}

    @GetMapping("/cart/getCartWithItems")
	public CartDto getCartWithItems(@RequestParam(value = "cartId") String cartId) {
		Cart cart = cartService.findById(cartId);
		List<CartItem> items = cartItemService.findByCartId(cartId);

        return new CartDto(cart, items);
	}

	@GetMapping("/cart/addItem")
	public CartDto addItem(@RequestParam(value = "cartId") String cartId, @RequestParam(value = "productId") String productId) {
		cartItemService.addItem(cartId, productId);
		return getCartWithItems(cartId);
	}

	@GetMapping("/cart/removeItem")
	public CartDto removeItem(@RequestParam(value = "cartId") String cartId, @RequestParam(value = "productId") String productId) {
		cartItemService.removeItem(cartId, productId);
		return getCartWithItems(cartId);
	}

}
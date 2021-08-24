package fi.hel.verkkokauppa.cart.api;

import fi.hel.verkkokauppa.cart.model.Cart;
import fi.hel.verkkokauppa.cart.model.CartItem;
import fi.hel.verkkokauppa.cart.service.CartItemService;
import fi.hel.verkkokauppa.cart.service.CartService;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CartController {
	private Logger log = LoggerFactory.getLogger(CartController.class);

	@Autowired
	private CartService cartService;

	@Autowired
	private CartItemService cartItemService;


	@GetMapping("/cart/create")
	public ResponseEntity<Cart> createCart(@RequestParam(value = "namespace") String namespace,
										   @RequestParam(value = "user") String user) {
		try {
			return ResponseEntity.ok().body(cartService.createByParams(namespace, user));
		} catch (Exception e) {
			log.error("creating cart failed", e);
			throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, new Error("failed-to-create-cart", "failed to create cart"));
		}
	}

	@GetMapping("/cart/get")
	public ResponseEntity<Cart> getCart(@RequestParam(value = "cartId") String cartId) {
		Cart cart = null;

		try {
			cart = cartService.findById(cartId);
		} catch (Exception e) {
			log.error("getting cart failed, cartId: " + cartId, e);
			throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, new Error("failed-to-get-cart", "failed to get cart with id [" + cartId + "]"));
		}

		if (cart == null) {
			throw new CommonApiException(HttpStatus.NOT_FOUND, new Error("cart-not-found", "cart with id [" + cartId + "] not found"));
		}

		return ResponseEntity.ok().body(cart);
	}

	@GetMapping("/cart/getByNames")
	public ResponseEntity<Cart> getCartByNames(@RequestParam(value = "namespace") String namespace,
											   @RequestParam(value = "user") String user) {
		Cart cart = null;

		try {
			cart = cartService.findByNamespaceAndUser(namespace, user);
		} catch (Exception e) {
			log.error("getting cart failed, namespace: " + namespace + "user: " + user, e);
			throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, new Error("failed-to-get-cart-by-names", "failed to get cart with namescapce and user"));
		}

		if (cart == null) {
			throw new CommonApiException(HttpStatus.NOT_FOUND, new Error("cart-not-found-by-names", "cart not found by namescapce and user"));
		}

		return ResponseEntity.ok().body(cart);
	}

	@GetMapping("/cart/getCartWithItems")
	public ResponseEntity<CartDto> getCartWithItems(@RequestParam(value = "cartId") String cartId) {
		try {
			Cart cart = cartService.findById(cartId);
			List<CartItem> items = cartItemService.findByCartId(cartId);

			return ResponseEntity.ok().body(new CartDto(cart, items));
		} catch (Exception e) {
			log.error("getting cart with items failed, cartId: " + cartId, e);
			Error error = new Error("failed-to-get-cart-with-items", "failed to get cart with id [" + cartId + "] with items");
			throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
		}
	}

	@GetMapping("/cart/addItem")
	public ResponseEntity<CartDto> addItem(@RequestParam(value = "cartId") String cartId, @RequestParam(value = "productId") String productId,
										   @RequestParam(value = "quantity", required = false, defaultValue = "1") Integer quantity) {
		try {
			cartItemService.addItem(cartId, productId, quantity);
			return getCartWithItems(cartId);
		} catch (Exception e) {
			log.error("adding cart item failed, cartId: " + cartId + ", productId: " + productId, e);
			Error error = new Error("failed-to-add-cart-item", "failed to add item [" + productId + "] to cart with id [" + cartId + "]");
			throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
		}
	}

	@GetMapping("/cart/removeItem")
	public ResponseEntity<CartDto> removeItem(@RequestParam(value = "cartId") String cartId, @RequestParam(value = "productId") String productId,
											  @RequestParam(value = "quantity", required = false, defaultValue = "0") Integer quantity) {
		try {
			cartItemService.removeItem(cartId, productId, quantity);
			return getCartWithItems(cartId);
		} catch (Exception e) {
			log.error("removing cart item failed, cartId: " + cartId + ", productId: " + productId, e);
			Error error = new Error("failed-to-remove-cart-item", "failed to remove item [" + productId + "] from cart with id [" + cartId + "]");
			throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
		}
	}

	@GetMapping("/cart/editItem")
	public ResponseEntity<CartDto> editItem(@RequestParam(value = "cartId") String cartId,
											@RequestParam(value = "productId") String productId, @RequestParam(value = "quantity") Integer quantity) {
		try {
			cartItemService.editItemQuantity(cartId, productId, quantity);
			return getCartWithItems(cartId);
		} catch (Exception e) {
			log.error("editing cart item quantity failed, cartId: " + cartId + ", productId: " + productId, e);
			Error error = new Error("failed-to-edit-cart-item", "failed to edit cart item [" + productId + "] quantity for cart with id [" + cartId + "]");
			throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
		}
	}

	@GetMapping("/cart/clear")
	public ResponseEntity<Cart> clearCart(@RequestParam(value = "namespace") String namespace, @RequestParam(value = "user") String user) {
		try {
			Cart cart = getCartByNames(namespace, user).getBody();
			if (cart != null) {
				cartItemService.removeAllByCartId(cart.getCartId());
			}

			return createCart(namespace, user);
		} catch (Exception e) {
			log.error("clearing cart failed, namespace: " + namespace + "user: " + user, e);
			throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, new Error("failed-to-clear-cart", "failed to clear cart"));
		}
	}

}

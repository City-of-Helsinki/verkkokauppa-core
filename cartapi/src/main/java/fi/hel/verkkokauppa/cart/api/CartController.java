package fi.hel.verkkokauppa.cart.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fi.hel.verkkokauppa.cart.model.Cart;
import fi.hel.verkkokauppa.cart.service.CartService;

@RestController
public class CartController {

    @Autowired
    private CartService service;


    @GetMapping("/cart/create")
	public Cart createServiceMapping(@RequestParam(value = "namespace") String namespace, 
            @RequestParam(value = "user") String user) {
		return service.createByParams(namespace, user);
	}
    
}
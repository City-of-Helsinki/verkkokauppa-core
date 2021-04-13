package fi.hel.verkkokauppa.product.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fi.hel.verkkokauppa.product.model.Product;
import fi.hel.verkkokauppa.product.service.ProductService;


@RestController
public class ProductController {

    @Autowired
    private ProductService service;


	@GetMapping("/product/get")
	public Product getProduct(@RequestParam(value = "productId") String productId) {
		return service.findById(productId);
	}

}
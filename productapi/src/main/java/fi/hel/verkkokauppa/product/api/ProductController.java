package fi.hel.verkkokauppa.product.api;

import fi.hel.verkkokauppa.product.constants.ApiUrls;
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


	@GetMapping(ApiUrls.PRODUCT_ROOT + "/get")
	public Product getProduct(@RequestParam(value = "productId") String productId) {
		return service.findById(productId);
	}

	@GetMapping(ApiUrls.PRODUCT_ROOT + "/getFromBackend")
	public Product getFromBackend(@RequestParam(value = "productId") String productId) {
		return service.getFromBackend(productId);
	}

}
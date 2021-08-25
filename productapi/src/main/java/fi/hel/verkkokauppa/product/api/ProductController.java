package fi.hel.verkkokauppa.product.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.product.constants.ApiUrls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fi.hel.verkkokauppa.product.model.Product;
import fi.hel.verkkokauppa.product.service.ProductService;


@RestController
public class ProductController {
	private Logger log = LoggerFactory.getLogger(ProductController.class);

	@Autowired
	private ProductService service;


	@GetMapping(ApiUrls.PRODUCT_ROOT + "/get")
	public ResponseEntity<Product> getProduct(@RequestParam(value = "productId") String productId) {
		Product product = null;

		try {
			product = service.findById(productId);
		} catch (Exception e) {
			log.error("getting product failed, productId: " + productId, e);
			Error error = new Error("failed-to-get-product", "failed to get product with id [" + productId + "]");
			throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
		}

		if (product == null) {
			throw new CommonApiException(HttpStatus.NOT_FOUND, new Error("product-not-found", "product with id [" + productId + "] not found"));
		}

		return ResponseEntity.ok().body(product);
	}

	@GetMapping(ApiUrls.PRODUCT_ROOT + "/getFromBackend")
	public ResponseEntity<Product> getFromBackend(@RequestParam(value = "productId") String productId) {
		Product product = null;

		try {
			product = service.getFromBackend(productId);
		} catch (Exception e) {
			log.error("getting product from backend failed, productId: " + productId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}

		if (product == null) {
			Error error = new Error("product-not-found-from-backend", "product with id [" + productId + "] not found from backend");
			throw new CommonApiException(HttpStatus.NOT_FOUND, error);

		}

		return ResponseEntity.ok().body(product);
	}

}

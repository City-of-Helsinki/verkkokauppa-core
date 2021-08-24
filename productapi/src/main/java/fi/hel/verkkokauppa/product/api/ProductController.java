package fi.hel.verkkokauppa.product.api;

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
		try {
			Product product = service.findById(productId);

			if (product == null) {
				return ResponseEntity.notFound().build();
			}

			return ResponseEntity.ok().body(product);
		} catch (Exception e) {
			log.error("getting product failed, productId: " + productId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GetMapping(ApiUrls.PRODUCT_ROOT + "/getFromBackend")
	public ResponseEntity<Product> getFromBackend(@RequestParam(value = "productId") String productId) {
		try {
			Product product = service.getFromBackend(productId);

			if (product == null) {
				return ResponseEntity.notFound().build();
			}

			return ResponseEntity.ok().body(product);
		} catch (Exception e) {
			log.error("getting product from backend failed, productId: " + productId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

}

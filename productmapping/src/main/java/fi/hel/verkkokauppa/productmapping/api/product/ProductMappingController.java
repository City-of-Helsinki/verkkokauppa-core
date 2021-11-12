package fi.hel.verkkokauppa.productmapping.api.product;

import java.util.List;

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

import fi.hel.verkkokauppa.productmapping.model.product.ProductMapping;
import fi.hel.verkkokauppa.productmapping.service.product.ProductMappingService;

@RestController
public class ProductMappingController {
	private Logger log = LoggerFactory.getLogger(ProductMappingController.class);

    @Autowired
    private ProductMappingService service;

    /**
     * Find the namespace and namespace specific product id via the common product id.
     * 
     * @param productId
     * @return ProductMapping with common productId as a UUID string and original backend identifiers
     */
	@GetMapping("/productmapping/get")
	public ResponseEntity<ProductMapping> getProductMapping(@RequestParam(value = "productId") String productId) {
		ProductMapping productMapping = null;

		try {
			productMapping = service.findById(productId);
		} catch (Exception e) {
			log.error("getting product mapping failed, productId: " + productId, e);
			Error error = new Error("failed-to-get-product mapping", "failed to get mapping for product [" + productId + "]");
			throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
		}

		if (productMapping == null) {
			throw new CommonApiException(HttpStatus.NOT_FOUND, new Error("product-mapping-not-found", "Mapping for product [" + productId + "] not found"));
		}

		return ResponseEntity.ok().body(productMapping);
	}

    /**
     * Generate a common product id for a business domain specific entity. 
     * For example for a product within tilavaraus namespace that is known within that namespace via it's local product id (namespaceEntityId). 
     * 
     * @param namespace
     * @param namespaceEntityId
     * @return ProductMapping with common productId as a UUID string and original backend identifiers
     */
    @GetMapping("/productmapping/create")
	public ResponseEntity<ProductMapping> createProductMapping(@RequestParam(value = "namespace") String namespace, @RequestParam(value = "namespaceEntityId") String namespaceEntityId) {
		try {
			return ResponseEntity.ok().body(service.createByParams(namespace, namespaceEntityId));
		} catch (Exception e) {
			log.error("creating product mapping failed", e);
			Error error = new Error("failed-to-create-product-mapping", "failed to create product mapping");
			throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);

		}
	}

    @GetMapping("/productmapping/initializetestdata")
	public ResponseEntity<List<ProductMapping>> initializeTestData() {
        try {
			return ResponseEntity.ok().body(service.initializeTestData());
		} catch (Exception e) {
			log.error("initializing product mapping test data failed", e);
			Error error = new Error("failed-to-create-product-mapping-test-data", "failed to initialize product mapping test data");
			throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
		}
	}
}
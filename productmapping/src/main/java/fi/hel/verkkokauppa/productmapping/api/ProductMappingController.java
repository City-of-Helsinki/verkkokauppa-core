package fi.hel.verkkokauppa.productmapping.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fi.hel.verkkokauppa.productmapping.model.ProductMapping;
import fi.hel.verkkokauppa.productmapping.service.ProductMappingService;

@RestController
public class ProductMappingController {

    @Autowired
    private ProductMappingService service;

    /**
     * Find the namespace and namespace specific product id via the common product id.
     * 
     * @param productId
     * @return ProductMapping with common productId as a UUID string and original backend identifiers
     */
	@GetMapping("/productmapping/get")
	public ProductMapping getProductMapping(@RequestParam(value = "productId") String productId) {
		return service.findById(productId);
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
	public ProductMapping createProductMapping(@RequestParam(value = "namespace") String namespace, @RequestParam(value = "namespaceEntityId") String namespaceEntityId) {
		return service.createByParams(namespace, namespaceEntityId);
	}

    @GetMapping("/productmapping/initializetestdata")
	public List<ProductMapping> initializeTestData() {
        return service.initializeTestData();
	}
}
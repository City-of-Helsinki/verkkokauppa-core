package fi.hel.verkkokauppa.product.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.product.constants.ApiUrls;
import fi.hel.verkkokauppa.product.model.Product;
import fi.hel.verkkokauppa.product.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


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
                Error error = new Error("product-not-found-from-backend", "product with id [" + productId + "] not found from backend");
                throw new CommonApiException(HttpStatus.NOT_FOUND, error);
            }

            return ResponseEntity.ok().body(product);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("getting product failed, productId: " + productId, e);
            Error error = new Error("failed-to-get-product", "failed to get product with id [" + productId + "]");
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
        }
    }

    /**
     * This resolves product by "productId" which is same as namespaceEntityId from productMappings
     * @param namespaceEntityId namespaceEntityId from productMappings
     */
    @GetMapping("product/get/internal")
    public ResponseEntity<Product> resolveProductFromInternalDatabase(@RequestParam(value = "productId") String namespaceEntityId) {
        try {
            Product product = service.resolveProductFromInternalDatabase(namespaceEntityId);

            if (product == null) {
                Error error = new Error("product-not-found-from-internal-backend", "product with id [" + namespaceEntityId + "] not found from backend");
                throw new CommonApiException(HttpStatus.NOT_FOUND, error);
            }

            return ResponseEntity.ok().body(product);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("getting product failed, productId: " + namespaceEntityId, e);
            Error error = new Error("failed-to-get-product", "failed to get product with id [" + namespaceEntityId + "]");
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
        }
    }

    @GetMapping(ApiUrls.PRODUCT_ROOT + "/getFromBackend")
    public ResponseEntity<Product> getFromBackend(@RequestParam(value = "productId") String productId) {
        try {
            Product product = service.getFromBackend(productId);

            if (product == null) {
                Error error = new Error("product-not-found-from-backend", "product with id [" + productId + "] not found from backend");
                throw new CommonApiException(HttpStatus.NOT_FOUND, error);
            }

            return ResponseEntity.ok().body(product);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("getting product from backend failed, productId: " + productId, e);
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, new Error("failed-to-get-product-from-backend", "failed to get product with id [" + productId + "] from backend "));
        }
    }

    @GetMapping(ApiUrls.PRODUCT_ROOT + "/createInternalProduct")
    public ResponseEntity<Product> createInternalProduct(
            @RequestParam(value = "productId") String productId,
            @RequestParam(value = "productName") String productName
    ) {
        try {
            Product product = service.createInternalProduct(productId, productName);

            if (product == null) {
                Error error = new Error("create-internal-product-failed", "product with id [" + productId + "] not found from backend");
                throw new CommonApiException(HttpStatus.NOT_FOUND, error);
            }

            return ResponseEntity.ok().body(product);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("getting product from backend failed, productId: " + productId, e);
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, new Error("failed-to-get-internal-product-from-backend", "failed to get internal product with id [" + productId + "] from backend "));
        }
    }

}

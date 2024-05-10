package fi.hel.verkkokauppa.price.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.price.model.Price;
import fi.hel.verkkokauppa.price.model.PriceModel;
import fi.hel.verkkokauppa.price.service.PriceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PriceController {
    private Logger log = LoggerFactory.getLogger(PriceController.class);

    @Autowired
    private PriceService service;


    @GetMapping("/price/get")
    public ResponseEntity<Price> getPrice(@RequestParam(value = "productId") String productId) {
        try {
            Price price = service.findByCommonProductId(productId);

            if (price == null) {
                Error error = new Error("price-not-found-from-backend", "price with product id [" + productId + "] not found from backend");
                throw new CommonApiException(HttpStatus.NOT_FOUND, error);
            }

            return ResponseEntity.ok().body(price);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("getting price failed, productId: " + productId, e);
            Error error = new Error("failed-to-get-price", "failed to get price for product [" + productId + "]");
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
        }
    }

    /**
     * This resolves price by "productId" which is same as namespaceEntityId from productMappings
     * @param namespaceEntityId namespaceEntityId from productMappings
     */
    @GetMapping("price/get/internal")
    public ResponseEntity<PriceModel> resolvePriceByNamespaceEntityId(@RequestParam(value = "productId") String namespaceEntityId) {
        try {
            PriceModel price = service.resolvePriceByNamespaceEntityId(namespaceEntityId);

            if (price == null) {
                Error error = new Error("price-not-found-from-backend", "price with product id [" + namespaceEntityId + "] not found from backend");
                throw new CommonApiException(HttpStatus.NOT_FOUND, error);
            }

            return ResponseEntity.ok().body(price);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("getting price failed, namespaceEntityId: " + namespaceEntityId, e);
            Error error = new Error("failed-to-get-price", "failed to get price for product [" + namespaceEntityId + "]");
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
        }
    }

    @GetMapping("/price/createInternalPrice")
    public ResponseEntity<PriceModel> createInternalPrice(
            @RequestParam(value = "productId") String productId,
            @RequestParam(value = "productPrice") String productPrice,
            @RequestParam(value = "productVatPercentage") String productVatPercentage
    ) {
        try {
            PriceModel price = service.findByCommonProductIdAndCreateInternalProduct(productId, productPrice, productVatPercentage);

            if (price == null) {
                Error error = new Error("price-not-found-from-backend", "price with product id [" + productId + "] not found from backend");
                throw new CommonApiException(HttpStatus.NOT_FOUND, error);
            }

            return ResponseEntity.ok().body(price);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("getting price failed, productId: " + productId, e);
            Error error = new Error("failed-to-create-internal-price", "failed to createInternalPrice price for product [" + productId + "]");
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
        }
    }

}

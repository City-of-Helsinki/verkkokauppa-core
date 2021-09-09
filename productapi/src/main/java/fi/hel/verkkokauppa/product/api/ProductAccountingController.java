package fi.hel.verkkokauppa.product.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.product.constants.ApiUrls;
import fi.hel.verkkokauppa.product.dto.GetProductAccountingListRequestDto;
import fi.hel.verkkokauppa.product.dto.ProductAccountingDto;
import fi.hel.verkkokauppa.product.model.ProductAccounting;
import fi.hel.verkkokauppa.product.service.ProductAccountingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class ProductAccountingController {

    private Logger log = LoggerFactory.getLogger(ProductAccountingController.class);

    @Autowired
    private ProductAccountingService productAccountingService;

    @PostMapping(ApiUrls.PRODUCT_ROOT + "/{productId}/" + ApiUrls.ACCOUNTING)
    public ResponseEntity<ProductAccounting> createProductAccounting(@PathVariable final String productId,
                                        @RequestBody ProductAccountingDto productAccountingDto)
    {
        try {
            productAccountingDto.setProductId(productId);

            final ProductAccounting productAccounting = productAccountingService.createProductAccounting(productAccountingDto);
            return ResponseEntity.ok().body(productAccounting);

        } catch (Exception e) {
            log.error("creating product accounting failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-create-product-accounting", "failed to create product accounting")
            );
        }
    }

    @GetMapping(ApiUrls.PRODUCT_ROOT + "/" + ApiUrls.ACCOUNTING + "/list")
    public ResponseEntity<List<ProductAccounting>> listProductAccountings(@RequestBody GetProductAccountingListRequestDto request) {
        try {
            List<ProductAccounting> productAccountings = new ArrayList<>();

            List<String> productIds = request.getProductIds();
            for (String productId : productIds) {
                ProductAccounting productAccounting = productAccountingService.getProductAccounting(productId);
                productAccountings.add(productAccounting);
            }
            return ResponseEntity.ok().body(productAccountings);

        } catch (Exception e) {
            log.error("listing product accountings failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-list-product-accountings", "failed to list product accountings")
            );
        }
    }

}

package fi.hel.verkkokauppa.product.api;

import fi.hel.verkkokauppa.product.constants.ApiUrls;
import fi.hel.verkkokauppa.product.dto.ProductAccountingDto;
import fi.hel.verkkokauppa.product.model.ProductAccounting;
import fi.hel.verkkokauppa.product.service.ProductAccountingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProductAccountingController {

    @Autowired
    private ProductAccountingService productAccountingService;

    @PostMapping(ApiUrls.PRODUCT_ROOT + "/{productId}/" + ApiUrls.ACCOUNTING)
    public ResponseEntity<ProductAccounting> createProductAccounting(@PathVariable final String productId,
                                        @RequestBody ProductAccountingDto productAccountingDto)
    {
        productAccountingDto.setProductId(productId);

        final ProductAccounting productAccounting = productAccountingService.createProductAccounting(productAccountingDto);
        ResponseEntity<ProductAccounting> response = ResponseEntity.ok().body(productAccounting);

        return response;
    }
}

package fi.hel.verkkokauppa.product.api;

import fi.hel.verkkokauppa.product.constants.ApiUrls;
import fi.hel.verkkokauppa.product.dto.ProductAccountingDto;
import fi.hel.verkkokauppa.product.service.ProductAccountingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProductAccountingController {

    @Autowired
    private ProductAccountingService productAccountingService;

    @PostMapping(ApiUrls.PRODUCT_ROOT + "/{productId}/" + ApiUrls.ACCOUNTING)
    public void createProductAccounting(@PathVariable final String productId,
                                        @RequestBody ProductAccountingDto productAccountingDto)
    {
        productAccountingDto.setProductId(productId);

        productAccountingService.createProductAccounting(productAccountingDto);
    }
}

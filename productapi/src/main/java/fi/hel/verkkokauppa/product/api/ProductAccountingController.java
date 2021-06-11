package fi.hel.verkkokauppa.product.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductAccountingController {


    @PostMapping("/product/{productId}/accounting")
    public void saveProductAccountingDetails(@PathVariable final String productId) {

    }
}

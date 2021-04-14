package fi.hel.verkkokauppa.price.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fi.hel.verkkokauppa.price.model.Price;
import fi.hel.verkkokauppa.price.service.PriceService;

@RestController
public class PriceController {
    
    @Autowired
    private PriceService service;


	@GetMapping("/price/get")
	public Price getPrice(@RequestParam(value = "productId") String productId) {
		return service.findByCommonProductId(productId);
	}

}

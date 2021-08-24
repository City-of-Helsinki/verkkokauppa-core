package fi.hel.verkkokauppa.price.api;

import fi.hel.verkkokauppa.price.model.Price;
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
			return ResponseEntity.ok().body(price);
		} catch (Exception e) {
			log.error("getting price failed, productId: " + productId, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

}

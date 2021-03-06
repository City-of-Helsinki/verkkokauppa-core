package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import fi.hel.verkkokauppa.payment.service.BillingPaymentService;

@RestController
public class BillingPaymentController {
        
    @Autowired
    private BillingPaymentService service;

	@PostMapping(value = "/payment/billing/createFromOrder", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> createPaymentFromOrder(@RequestBody GetPaymentRequestDataDto dto) {
		String redirectUrl = service.createFromOrder(dto);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(redirectUrl);
	}

}

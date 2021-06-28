package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OnlinePaymentController {
    
    @Autowired
    private OnlinePaymentService service;

	@PostMapping(value = "/payment/online/createFromOrder", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> createPaymentFromOrder(@RequestBody GetPaymentRequestDataDto dto) {
		String redirectUrl = service.getPaymentRequestData(dto);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(redirectUrl);
	}

}

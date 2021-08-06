package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.payment.api.data.GetPaymentMethodListRequest;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentMethodDto;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import fi.hel.verkkokauppa.payment.service.PaymentMethodListService;
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

	@Autowired
	private PaymentMethodListService paymentMethodListService;

	@PostMapping(value = "/payment/online/createFromOrder", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> createPaymentFromOrder(@RequestBody GetPaymentRequestDataDto dto) {
		String redirectUrl = service.getPaymentRequestData(dto);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(redirectUrl);
	}

	@PostMapping(value = "/payment/online/get-available-methods", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PaymentMethodDto[]> getAvailableMethods(@RequestBody GetPaymentMethodListRequest request) {
		PaymentMethodDto[] methods = paymentMethodListService.getPaymentMethodList(request.getCurrency());
		// TODO: check methods are active?
		// TODO: check if is available and can be used for this request dto.

		return ResponseEntity.status(HttpStatus.OK)
				.body(methods);
	}
}

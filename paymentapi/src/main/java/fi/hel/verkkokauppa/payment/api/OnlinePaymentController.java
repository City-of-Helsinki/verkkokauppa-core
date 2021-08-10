package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.payment.api.data.GetPaymentMethodListRequest;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentMethodDto;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import fi.hel.verkkokauppa.payment.service.PaymentMethodListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OnlinePaymentController {
    
    @Autowired
    private OnlinePaymentService service;

	@Autowired
	private PaymentMethodListService paymentMethodListService;

	@PostMapping(value = "/payment/online/createFromOrder", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Payment> createPaymentFromOrder(@RequestBody GetPaymentRequestDataDto dto) {
		Payment payment = service.getPaymentRequestData(dto);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(payment);
	}

	@PostMapping(value = "/payment/online/get-available-methods", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PaymentMethodDto[]> getAvailableMethods(@RequestBody GetPaymentMethodListRequest request) {
		PaymentMethodDto[] methods = paymentMethodListService.getPaymentMethodList(request.getCurrency());
		// TODO: check methods are active?
		// TODO: check if is available and can be used for this request dto.

		return ResponseEntity.status(HttpStatus.OK)
				.body(methods);
	}

	@GetMapping("/payment/online/url")
	public ResponseEntity<String> getPaymentUrl(@RequestParam(value = "namespace") String namespace, @RequestParam(value = "orderId") String orderId) {
		String paymentUrl = service.getPaymentUrl(namespace, orderId);

		return ResponseEntity.status(HttpStatus.OK)
				.body(paymentUrl);
	}

	@GetMapping("/payment/online/status")
	public ResponseEntity<String> getPaymentStatus(@RequestParam(value = "namespace") String namespace, @RequestParam(value = "orderId") String orderId) {
		String paymentStatus = service.getPaymentStatus(namespace, orderId);

		return ResponseEntity.status(HttpStatus.OK)
				.body(paymentStatus);
	}
	
}

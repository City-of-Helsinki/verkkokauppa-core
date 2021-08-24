package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaymentController {
    
    @Autowired
    private OnlinePaymentService onlinePaymentService;


	@GetMapping(value = "/payment/get", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Payment> getPayment(@RequestParam(value = "orderId") String orderId) {
		Payment payment = onlinePaymentService.getPaymentForOrder(orderId);

		return ResponseEntity.status(HttpStatus.OK)
				.body(payment);
	}

}

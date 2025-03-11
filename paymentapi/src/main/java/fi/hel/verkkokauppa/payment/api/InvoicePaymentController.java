package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentReturnDto;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import fi.hel.verkkokauppa.payment.service.InvoicePaymentService;

@RestController
@Slf4j
public class InvoicePaymentController {

    @Autowired
    private InvoicePaymentService service;

	@Autowired
	private OnlinePaymentService onlinePaymentService;

	@PostMapping(value = "/payment/invoice/createFromOrder", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Payment> createPaymentFromOrder(@RequestBody GetPaymentRequestDataDto dto) {
		try {
			Payment payment = service.createFromOrder(dto);
			return ResponseEntity.status(HttpStatus.CREATED).body(payment);
		} catch (CommonApiException cae) {
			throw cae;
		} catch (Exception e) {
			log.error("creating payment failed", e);
			throw new CommonApiException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					new Error("failed-to-create-payment", "failed to create payment")
			);
		}
	}

	@GetMapping("/payment/invoice/url")
	public ResponseEntity<String> getPaymentUrl(
			@RequestParam(value = "namespace") String namespace,
			@RequestParam(value = "orderId") String orderId,
			@RequestParam(value = "userId") String userId
	) {
		try {
			Payment payment = onlinePaymentService.findByIdValidateByUser(namespace, orderId, userId);
			String paymentUrl = service.getPaymentUrl(payment);
			return ResponseEntity.status(HttpStatus.OK).body(paymentUrl);
		} catch (CommonApiException cae) {
			throw cae;
		} catch (Exception e) {
			log.error("getting payment url failed, orderId: " + orderId, e);
			throw new CommonApiException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					new Error("failed-to-get-paytrail-payment-url", "failed to get paytrail payment url with order id [" + orderId + "]")
			);
		}
	}

	@GetMapping("/payment/invoice/check-return-url")
	public ResponseEntity<PaymentReturnDto> checkReturnUrl(
			@RequestParam(value = "merchantId") String merchantId,
			@RequestParam(value = "orderId") String orderId
	) {
		try {
			Payment payment = onlinePaymentService.getPaymentForOrder(orderId);
			if( payment == null ){
				log.error("No payable payment found for free payment");
				throw new CommonApiException(
						HttpStatus.INTERNAL_SERVER_ERROR,
						new Error("failed-to-check-payment-return-response", "failed to check payment return response")
				);
			}
			// validate payment
			boolean isValid = service.validatePayment(payment);
			// no status checks needed, just create dto
			PaymentReturnDto paymentReturnDto = service.createPaymentReturnDto(isValid);
			// update payment and trigger payment paid event (webhook)
			onlinePaymentService.updatePaymentStatus(payment.getPaymentId(), paymentReturnDto);
			paymentReturnDto.setPaymentType(payment.getPaymentType());

			return ResponseEntity.status(HttpStatus.OK).body(paymentReturnDto);
		} catch (CommonApiException cae) {
			throw cae;
		} catch (Exception e) {
			log.error("checking payment return response for invoice failed", e);
			throw new CommonApiException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					new Error("failed-to-check-payment-return-response", "failed to check payment return response")
			);
		}
	}
}

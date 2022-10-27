package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.util.ListUtil;
import fi.hel.verkkokauppa.payment.api.data.*;
import fi.hel.verkkokauppa.payment.logic.validation.PaymentReturnValidator;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import fi.hel.verkkokauppa.payment.service.PaymentMethodService;
import fi.hel.verkkokauppa.payment.service.PaymentPaytrailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private Logger log = LoggerFactory.getLogger(OnlinePaymentController.class);

	@Autowired
    private OnlinePaymentService service;

	@Autowired
	private PaymentMethodService paymentMethodService;

	@Autowired
	private PaymentReturnValidator paymentReturnValidator;

	@Autowired
	private PaymentPaytrailService paymentPaytrailService;


	@PostMapping(value = "/payment/online/createFromOrder", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Payment> createPaymentFromOrder(@RequestBody GetPaymentRequestDataDto dto) {
		try {
			Payment payment = service.getPaymentRequestData(dto);
			return ResponseEntity.status(HttpStatus.CREATED).body(payment);
		} catch (CommonApiException cae) {
			throw cae;
		} catch (Exception e) {
			log.error("creating payment or chargerequest failed", e);
			throw new CommonApiException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					new Error("failed-to-create-payment", "failed to create payment")
			);
		}
	}

	@GetMapping(value = "/payment/online/visma/get", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Payment> getVismaPaymentRequest(@RequestParam(value = "namespace") String namespace, @RequestParam(value = "orderId") String orderId,
														  @RequestParam(value = "userId") String userId) {
		try {
			Payment payment = service.findByIdValidateByUser(namespace, orderId, userId);

			// TODO haku
			return ResponseEntity.status(HttpStatus.OK).body(payment);
		} catch (CommonApiException cae) {
			throw cae;
		} catch (Exception e) {
			log.error("getting payment failed, orderId: " + orderId, e);
			throw new CommonApiException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					new Error("failed-to-get-payment", "failed to get payment with order id [" + orderId + "]")
			);
		}
	}



	@PostMapping(value = "/payment/online/get-available-methods", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PaymentMethodDto[]> getAvailableMethods(@RequestBody GetPaymentMethodListRequest request) {
		try {
			String namespace = request.getNamespace();

			PaymentMethodDto[] vismaMethods = paymentMethodService.getOnlinePaymentMethodList(request.getCurrency());
			PaymentMethodDto[] paytrailMethods = paymentPaytrailService.getOnlinePaymentMethodList(request.getMerchantId(),
					request.getNamespace(), request.getCurrency());
			PaymentMethodDto[] allMethods = ListUtil.mergeArrays(PaymentMethodDto.class, vismaMethods, paytrailMethods);

			// TODO: check methods are active?
			// TODO: check if is available and can be used for this request dto.

			allMethods = paymentMethodService.filterPaymentMethodList(request, allMethods);

			if (allMethods.length == 0) {
				log.debug("payment methods not found, namespace: " + namespace);
				Error error = new Error("payment-methods-not-found-from-backend", "payment methods for namespace[" + namespace + "] not found from backend");
				throw new CommonApiException(HttpStatus.NOT_FOUND, error);
			}

			return ResponseEntity.status(HttpStatus.OK).body(allMethods);
		} catch (CommonApiException cae) {
			throw cae;
		} catch (Exception e) {
			log.error("getting payment methods failed", e);
			throw new CommonApiException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					new Error("failed-to-get-payment-methods", "failed to get payment methods")
			);
		}
	}

	@GetMapping(value = "/payment/online/get", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Payment> getPayment(@RequestParam(value = "namespace") String namespace, @RequestParam(value = "orderId") String orderId,
														  @RequestParam(value = "userId") String userId) {
		try {
			Payment payment = service.findByIdValidateByUser(namespace, orderId, userId);
			return ResponseEntity.status(HttpStatus.OK).body(payment);
		} catch (CommonApiException cae) {
			throw cae;
		} catch (Exception e) {
			log.error("getting payment failed, orderId: " + orderId, e);
			throw new CommonApiException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					new Error("failed-to-get-payment", "failed to get payment with order id [" + orderId + "]")
			);
		}
	}

	@GetMapping("/payment/online/url")
	public ResponseEntity<String> getPaymentUrl(@RequestParam(value = "namespace") String namespace, @RequestParam(value = "orderId") String orderId,
												@RequestParam(value = "userId") String userId) {
		try {
			Payment payment = service.findByIdValidateByUser(namespace, orderId, userId);
			String paymentUrl = service.getPaymentUrl(payment);
			return ResponseEntity.status(HttpStatus.OK).body(paymentUrl);
		} catch (CommonApiException cae) {
			throw cae;
		} catch (Exception e) {
			log.error("getting payment url failed, orderId: " + orderId, e);
			throw new CommonApiException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					new Error("failed-to-get-payment-url", "failed to get payment url with order id [" + orderId + "]")
			);
		}
	}

	@GetMapping("/payment/online/status")
	public ResponseEntity<String> getPaymentStatus(@RequestParam(value = "namespace") String namespace, @RequestParam(value = "orderId") String orderId,
												   @RequestParam(value = "userId") String userId) {
		try {
			Payment payment = service.findByIdValidateByUser(namespace, orderId, userId);
			return ResponseEntity.status(HttpStatus.OK).body(payment.getStatus());
		} catch (CommonApiException cae) {
			throw cae;
		} catch (Exception e) {
			log.error("getting payment status failed, orderId: " + orderId, e);
			throw new CommonApiException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					new Error("failed-to-get-payment-status", "failed to get payment status with order id [" + orderId + "]")
			);
		}
	}

	@GetMapping(value = "/payment/online/cardInfo", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PaymentCardInfoDto> getPaymentCardInfo(@RequestParam(value = "namespace") String namespace, @RequestParam(value = "orderId") String orderId,
																 @RequestParam(value = "userId") String userId) {
		try {
			Payment payment = service.findByIdValidateByUser(namespace, orderId, userId);
			String paymentId = payment.getPaymentId();
			PaymentCardInfoDto paymentCardToken = service.getPaymentCardToken(paymentId);

			return ResponseEntity.ok().body(paymentCardToken);
		} catch (CommonApiException cae) {
			throw cae;
		} catch (Exception e) {
			log.error("getting payment card token failed, orderId: " + orderId, e);
			throw new CommonApiException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					new Error("failed-to-get-payment-card-token", "failed to get payment card token with order id [" + orderId + "]")
			);
		}
	}

	@GetMapping("/payment/online/check-return-url")
	public ResponseEntity<PaymentReturnDto> checkReturnUrl(@RequestParam(value = "AUTHCODE") String authCode, @RequestParam(value = "RETURN_CODE") String returnCode,
		@RequestParam(value = "ORDER_NUMBER") String paymentId, @RequestParam(value = "SETTLED", required = false) String settled, @RequestParam(value = "INCIDENT_ID", required = false) String incidentId) {
		try {
			boolean isValid = paymentReturnValidator.validateChecksum(authCode, returnCode, paymentId, settled, incidentId);
			PaymentReturnDto paymentReturnDto = paymentReturnValidator.validateReturnValues(isValid, returnCode, settled);
			service.updatePaymentStatus(paymentId, paymentReturnDto);
			Payment payment = service.getPayment(paymentId);
			paymentReturnDto.setPaymentType(payment.getPaymentType());
			return ResponseEntity.status(HttpStatus.OK).body(paymentReturnDto);
		} catch (CommonApiException cae) {
			throw cae;
		} catch (Exception e) {
			log.error("checking payment return response failed", e);
			throw new CommonApiException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					new Error("failed-to-check-payment-return-response", "failed to check payment return response")
			);
		}
	}


}

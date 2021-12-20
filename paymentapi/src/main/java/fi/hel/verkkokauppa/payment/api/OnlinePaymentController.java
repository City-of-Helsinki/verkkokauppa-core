package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.constants.PaymentType;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.SendEventService;
import fi.hel.verkkokauppa.common.events.TopicName;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.util.EncryptorUtil;
import fi.hel.verkkokauppa.payment.api.data.*;
import fi.hel.verkkokauppa.payment.logic.PaymentReturnValidator;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.model.PaymentStatus;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import fi.hel.verkkokauppa.payment.service.PaymentMethodListService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class OnlinePaymentController {

	private Logger log = LoggerFactory.getLogger(OnlinePaymentController.class);

	@Autowired
    private OnlinePaymentService service;

	@Autowired
	private PaymentMethodListService paymentMethodListService;

	@Autowired
	private PaymentReturnValidator paymentReturnValidator;


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

	@PostMapping(value = "/payment/online/create/card-renewal-payment", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Payment> createCardRenewalPayment(@RequestBody GetPaymentRequestDataDto dto) {
		try {

			Payment payment = service.getPaymentRequestDataForCardRenewal(dto);
			return ResponseEntity.status(HttpStatus.CREATED).body(payment);
		} catch (CommonApiException cae) {
			throw cae;
		} catch (Exception e) {
			log.error("creating payment or card-renewal-payment failed", e);
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
			PaymentMethodDto[] methods = paymentMethodListService.getPaymentMethodList(request.getCurrency());
			// TODO: check methods are active?
			// TODO: check if is available and can be used for this request dto.

			methods = paymentMethodListService.filterPaymentMethodList(request, methods);

			if (methods.length == 0) {
				log.debug("payment methods not found, namespace: " + namespace);
				Error error = new Error("payment-methods-not-found-from-backend", "payment methods for namespace[" + namespace + "] not found from backend");
				throw new CommonApiException(HttpStatus.NOT_FOUND, error);
			}

			return ResponseEntity.status(HttpStatus.OK).body(methods);
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
			String paymentToken = payment.getToken();
			PaymentCardInfoDto paymentCardToken = service.getPaymentCardToken(paymentToken);

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

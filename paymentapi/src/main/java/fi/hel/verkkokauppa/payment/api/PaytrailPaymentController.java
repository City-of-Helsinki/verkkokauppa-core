package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentReturnDto;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.paytrail.validation.PaytrailPaymentReturnValidator;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import fi.hel.verkkokauppa.payment.service.PaymentPaytrailService;
import fi.hel.verkkokauppa.payment.util.PaymentUtil;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.model.payments.PaytrailPaymentMitChargeSuccessResponse;
import org.helsinki.paytrail.model.tokenization.PaytrailTokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.TreeMap;

@RestController
@Slf4j
public class PaytrailPaymentController {

    @Autowired
    private PaymentPaytrailService paymentPaytrailService;

    @Autowired
    private OnlinePaymentService onlinePaymentService;

    @Autowired
    private PaytrailPaymentReturnValidator paytrailPaymentReturnValidator;

    @Autowired
    private Environment env;

    @PostMapping("/payment/paytrail/createFromOrder")
    public ResponseEntity<Payment> createPaymentFromOrder(@RequestBody GetPaymentRequestDataDto dto) {
        try {
            Payment payment = paymentPaytrailService.getPaymentRequestData(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(payment);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("creating payment or paytrail payment request failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-create-paytrail-payment", "failed to create paytrail payment")
            );
        }
    }

    @PostMapping("/payment/paytrail/check-card-return-url")
    public ResponseEntity<Payment> checkCardReturnUrl(
            @RequestBody GetPaymentRequestDataDto dto,
            @RequestParam Map<String, String> params,
            @RequestParam(value = "signature") String signature,
            @RequestParam(value = "checkout-tokenization-id") String tokenizationId
    ) {
        try {
            OrderDto orderDto = dto.getOrder().getOrder();
            paymentPaytrailService.validateOrder(orderDto);

            String namespace = orderDto.getNamespace();
            String merchantId = dto.getMerchantId();
            paytrailPaymentReturnValidator.validateSignature(merchantId, params, signature);
            PaytrailPaymentContext context = paymentPaytrailService.buildPaytrailContext(namespace, merchantId);
            PaytrailTokenResponse card = paymentPaytrailService.getToken(context, tokenizationId);
            String paymentId = PaymentUtil.generatePaymentOrderNumber(orderDto.getOrderId());
            PaytrailPaymentMitChargeSuccessResponse mitCharge = paymentPaytrailService.createMitCharge(context, paymentId, dto.getOrder(), card.getToken());
            Payment payment = paymentPaytrailService.createPayment(context, dto, paymentId, mitCharge);
            paymentPaytrailService.triggerPaymentPaidEvent(payment, card);
            return ResponseEntity.status(HttpStatus.OK).body(payment);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("checking card return response failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-check-card-return-response", "failed to check card return response")
            );
        }
    }

    @GetMapping("/payment/paytrail/check-return-url")
    public ResponseEntity<PaymentReturnDto> checkReturnUrl(
            @RequestParam(value = "merchantId") String merchantId,
            @RequestParam(value = "signature") String signature,
            @RequestParam(value = "checkout-status") String status,
            @RequestParam(value = "checkout-stamp") String paymentId,
            @RequestParam(value = "checkout-settlement-reference", required = false) String settlementReference,
            @RequestParam Map<String, String> checkoutParams
    ) {
        try {
            boolean isValid = paytrailPaymentReturnValidator.validateChecksum(checkoutParams, merchantId, signature, paymentId);
            PaymentReturnDto paymentReturnDto = paytrailPaymentReturnValidator.validateReturnValues(isValid, status, settlementReference);
            onlinePaymentService.updatePaymentStatus(paymentId, paymentReturnDto);
            Payment payment = onlinePaymentService.getPayment(paymentId);
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

    @GetMapping("/payment/paytrail/url")
    public ResponseEntity<String> getPaymentUrl(
            @RequestParam(value = "namespace") String namespace,
            @RequestParam(value = "orderId") String orderId,
            @RequestParam(value = "userId") String userId
    ) {
        try {
            Payment payment = onlinePaymentService.findByIdValidateByUser(namespace, orderId, userId);
            String paymentUrl = paymentPaytrailService.getPaymentUrl(payment);
            return ResponseEntity.status(HttpStatus.OK).body(paymentUrl);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("getting paytrail payment url failed, orderId: " + orderId, e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-paytrail-payment-url", "failed to get paytrail payment url with order id [" + orderId + "]")
            );
        }
    }

    @GetMapping("/subscription/get/card-form-parameters")
    public ResponseEntity<TreeMap<String, String>> getCardFormParameters(
            @RequestParam(value = "merchantId") String merchantId,
            @RequestParam(value = "namespace") String namespace,
            @RequestParam(value = "orderId") String orderId
    ) {
        try {
            TreeMap<String, String> parameters = paymentPaytrailService.getCardReturnParameters(
                    merchantId,
                    namespace,
                    orderId
            );

            return ResponseEntity.status(HttpStatus.OK).body(parameters);
        } catch (CommonApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("getting card form parameters failed, merchantId: " + merchantId);
            Error error = new Error("failed-to-get-card-form-parameters", "failed to get card form parameters [" + merchantId + ", " + namespace + "]");
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
        }
    }

    @GetMapping("/subscription/get/update-card-form-parameters")
    public ResponseEntity<TreeMap<String, String>> getUpdateCardFormParameters(
            @RequestParam(value = "merchantId") String merchantId,
            @RequestParam(value = "namespace") String namespace,
            @RequestParam(value = "orderId") String orderId
    ) {
        try {

            TreeMap<String, String> parameters = paymentPaytrailService.getUpdateCardReturnParameters(
                    merchantId,
                    namespace,
                    orderId
            );

            return ResponseEntity.status(HttpStatus.OK).body(parameters);
        } catch (CommonApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("getting update card form parameters failed, merchantId: " + merchantId);
            Error error = new Error("failed-to-get-update-card-form-parameters", "failed to get update card form parameters [" + merchantId + ", " + namespace + "]");
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
        }
    }

    @PostMapping("/payment/paytrail/check-card-update-return-url")
    public ResponseEntity<Void> checkCardUpdateReturnUrl(
            @RequestBody GetPaymentRequestDataDto dto,
            @RequestParam Map<String, String> params,
            @RequestParam(value = "signature") String signature,
            @RequestParam(value = "checkout-tokenization-id") String tokenizationId
    ) {
        try {
            String namespace = dto.getOrder().getOrder().getNamespace();
            String merchantId = PaymentUtil.parseMerchantId(dto.getOrder());

            paytrailPaymentReturnValidator.validateSignature(merchantId, params, signature);
            PaytrailPaymentContext context = paymentPaytrailService.buildPaytrailContext(namespace, merchantId);
            PaytrailTokenResponse card = paymentPaytrailService.getToken(context, tokenizationId);

            paymentPaytrailService.triggerCardUpdateEvent(dto.getOrder().getOrder(), card);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("checking paytrail card update return response failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-check-card-update-return-response", "failed to check card update return response")
            );
        }
    }
}

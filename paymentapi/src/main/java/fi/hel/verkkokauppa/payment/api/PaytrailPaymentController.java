package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentReturnDto;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.paytrail.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.paytrail.validation.PaytrailPaymentReturnValidator;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import fi.hel.verkkokauppa.payment.service.PaymentPaytrailService;
import fi.hel.verkkokauppa.payment.util.PaymentUtil;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.constants.CheckoutAlgorithm;
import org.helsinki.paytrail.constants.CheckoutMethod;
import org.helsinki.paytrail.model.payments.PaytrailPaymentMitChargeSuccessResponse;
import org.helsinki.paytrail.model.tokenization.PaytrailTokenResponse;
import org.helsinki.paytrail.service.PaytrailSignatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
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
    private CommonServiceConfigurationClient commonServiceConfigurationClient;

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
            String namespace = dto.getOrder().getOrder().getNamespace();
            String merchantId = dto.getMerchantId();
            paytrailPaymentReturnValidator.validateSignature(merchantId, params, signature);
            PaytrailPaymentContext context = paymentPaytrailService.buildPaytrailContext(namespace, merchantId);
            PaytrailTokenResponse card = paymentPaytrailService.getToken(context, tokenizationId);
            paymentPaytrailService.validateOrder(dto.getOrder().getOrder());
            String paymentId = PaymentUtil.generatePaymentOrderNumber(dto.getOrder().getOrder().getOrderId());
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
            @RequestParam Map<String,String> checkoutParams
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
            String paytrailMerchantId = commonServiceConfigurationClient.getMerchantConfigurationValue(merchantId, namespace, ServiceConfigurationKeys.MERCHANT_PAYTRAIL_MERCHANT_ID);

            if (paytrailMerchantId == null) {
                throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-paytrail-merchant-id", "failed to get paytrail merchant id, merchantId: " + merchantId + ", namespace: " + namespace)
                );
            }

            String secretKey = commonServiceConfigurationClient.getMerchantPaytrailSecretKey(merchantId);

            if (secretKey == null) {
                throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-merchant-paytrail-secret-key", "failed to get paytrail secret key, merchantId: " + merchantId)
                );
            }

            String redirectSuccessUrl = env.getRequiredProperty("paytrail_card_redirect_success_url");
            String redirectCancelUrl = env.getRequiredProperty("paytrail_card_redirect_cancel_url");
            String callbackSuccessUrl = env.getRequiredProperty("paytrail_card_callback_success_url");
            String callbackCancelUrl = env.getRequiredProperty("paytrail_card_callback_cancel_url");

            TreeMap<String, String> parameters = new TreeMap<>();
            parameters.put("checkout-account", paytrailMerchantId);
            parameters.put("checkout-algorithm", CheckoutAlgorithm.SHA256.toString());
            parameters.put("checkout-method", CheckoutMethod.POST.toString());
            parameters.put("checkout-nonce", UUIDGenerator.generateType4UUID().toString());
            parameters.put("checkout-timestamp", Instant.now().toString());
            parameters.put("checkout-redirect-success-url", redirectSuccessUrl + (redirectSuccessUrl.endsWith("/") ? "" : "/") + orderId);
            parameters.put("checkout-redirect-cancel-url", redirectCancelUrl + (redirectCancelUrl.endsWith("/") ? "" : "/") + orderId);
            parameters.put("checkout-callback-success-url", callbackSuccessUrl + (callbackSuccessUrl.endsWith("/") ? "" : "/") + orderId);
            parameters.put("checkout-callback-cancel-url", callbackCancelUrl + (callbackCancelUrl.endsWith("/") ? "" : "/") + orderId);
            parameters.put("signature", PaytrailSignatureService.calculateSignature(parameters, null, secretKey));

            return ResponseEntity.status(HttpStatus.OK).body(parameters);
        } catch (CommonApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("getting card form parameters failed, merchantId: " + merchantId);
            Error error = new Error("failed-to-get-card-form-parameters", "failed to get card form parameters [" + merchantId + ", " + namespace + "]");
            throw new CommonApiException(HttpStatus.INTERNAL_SERVER_ERROR, error);
        }
    }
}

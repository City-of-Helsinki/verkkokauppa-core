package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.common.configuration.ExperienceUrls;
import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentReturnDto;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.paytrail.validation.PaytrailPaymentReturnValidator;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import fi.hel.verkkokauppa.payment.service.PaymentPaytrailService;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.constants.CheckoutAlgorithm;
import org.helsinki.paytrail.constants.CheckoutMethod;
import org.helsinki.paytrail.service.PaytrailSignatureService;
import org.springframework.beans.factory.annotation.Autowired;
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
    private ExperienceUrls experienceUrls;

    @Autowired
    private CommonServiceConfigurationClient commonServiceConfigurationClient;

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
            @RequestParam(value = "namespace") String namespace
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


            String paymentExperienceUrl = experienceUrls.getPaymentExperienceUrl();

            TreeMap<String, String> parameters = new TreeMap<>();
            parameters.put("checkout-account", paytrailMerchantId);
            parameters.put("checkout-algorithm", CheckoutAlgorithm.SHA256.toString());
            parameters.put("checkout-method", CheckoutMethod.POST.toString());
            parameters.put("checkout-nonce", UUIDGenerator.generateType4UUID().toString());
            parameters.put("checkout-timestamp", Instant.now().toString());
            parameters.put("checkout-redirect-success-url", String.format("%spaytrailCard/redirect/success", paymentExperienceUrl));
            parameters.put("checkout-redirect-cancel-url", String.format("%spaytrailCard/redirect/cancel", paymentExperienceUrl));
            parameters.put("checkout-callback-success-url", String.format("%spaytrailCard/callback/success", paymentExperienceUrl));
            parameters.put("checkout-callback-cancel-url", String.format("%spaytrailCard/callback/cancel", paymentExperienceUrl));
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

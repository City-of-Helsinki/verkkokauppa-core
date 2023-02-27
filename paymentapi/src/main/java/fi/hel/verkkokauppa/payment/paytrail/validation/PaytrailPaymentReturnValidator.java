package fi.hel.verkkokauppa.payment.paytrail.validation;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.util.StringUtils;
import fi.hel.verkkokauppa.payment.api.data.PaymentReturnDto;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.constants.PaymentStatus;
import org.helsinki.paytrail.service.PaytrailSignatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

@Component
@Slf4j
public class PaytrailPaymentReturnValidator {

    @Value("${paytrail.aggregate.merchant.secret:}")
    private String aggregateSecretKey;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CommonServiceConfigurationClient commonServiceConfigurationClient;


    public boolean validateChecksum(Map<String,String> checkoutParams, String merchantId, String signature, String paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> new CommonApiException(
                HttpStatus.NOT_FOUND,
                new Error("payment-not-found", "payment with id [" + paymentId + "] not found")
        ));
        String createdSignature = buildSignatureFromReturnData(checkoutParams, merchantId, payment.isShopInShopPayment());
        if (createdSignature.equals(signature)) {
            return true;
        } else {
            log.debug("paytrail payment validation failed, orderNumber: " + paymentId);
            return false;
        }
    }

    public PaymentReturnDto validateReturnValues(boolean isValid, String status, String settlementReference) {
        boolean isPaymentPaid = false;
        boolean canRetry = false;
        boolean isAuthorized = false;

        if (isSuccesfull(status)) {
            isPaymentPaid = true;
        } else if (isSuccesfull(status) && StringUtils.isEmpty(settlementReference)){
            // This is temporarily set to FALSE until subscription flow is fully implemented.
            // When subscriptions are supported, change this to TRUE
            //isAuthorized = false;
        } else {
            if (!PaymentStatus.PENDING.getStatus().equals(status)) {
                canRetry = true;
            }
        }

        return new PaymentReturnDto(isValid, isPaymentPaid, canRetry, isAuthorized);
    }

    private boolean isSuccesfull(String status) {
        return PaymentStatus.OK.getStatus().equals(status);
    }

    public void validateSignature(String merchantId, Map<String,String> params, String signature) {
        boolean signatureIsValid = signature.equals(buildSignatureFromReturnData(params, merchantId, false));
        if (!signatureIsValid) {
            throw new CommonApiException(
                    HttpStatus.FORBIDDEN,
                    new Error("invalid-signature",
                            "paytrail signature is invalid")
            );
        }
    }

    private String buildSignatureFromReturnData(Map<String,String> checkoutParams, String merchantId, boolean isShopInShopPayment) {
        TreeMap<String, String> checkoutSignatureParameters = PaytrailSignatureService.filterCheckoutQueryParametersMap(checkoutParams);

        try {
            String calculatedSignature;
            if (isShopInShopPayment) {
                calculatedSignature = PaytrailSignatureService.calculateSignature(checkoutSignatureParameters, null, aggregateSecretKey);
            } else {
                String merchantSecretkey = commonServiceConfigurationClient.getMerchantPaytrailSecretKey(merchantId);
                if (StringUtils.isEmpty(merchantSecretkey)) {
                    log.debug("No paytrail secret key found for merchant {}", merchantId);
                    return null;
                }
                calculatedSignature = PaytrailSignatureService.calculateSignature(checkoutSignatureParameters, null, merchantSecretkey);
            }
            log.debug("calculatedSignature: " + calculatedSignature);
            return calculatedSignature;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.debug("Failed to create signature from return data", e);
            log.debug(e.getMessage());
            return null;
        }
    }
}

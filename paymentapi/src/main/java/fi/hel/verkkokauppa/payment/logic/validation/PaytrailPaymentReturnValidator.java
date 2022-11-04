package fi.hel.verkkokauppa.payment.logic.validation;

import fi.hel.verkkokauppa.common.util.StringUtils;
import fi.hel.verkkokauppa.payment.api.data.PaymentReturnDto;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.constants.PaymentStatus;
import org.helsinki.paytrail.service.PaytrailSignatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

@Component
@Slf4j
public class PaytrailPaymentReturnValidator {

    @Value("${paytrail.merchant.secret:}")
    private String secretKey;


    public boolean validateChecksum(Map<String,String> checkoutParams, String signature, String orderNumber) {
        if (buildSignatureFromReturnData(checkoutParams).equals(signature)) {
            return true;
        } else {
            log.debug("paytrail payment validation failed, orderNumber: " + orderNumber);
            return false;
        }
    }

    public PaymentReturnDto validateReturnValues(boolean isValid, String status, String settlementReference) {
        boolean isPaymentPaid = false;
        boolean canRetry = false;
        boolean isAuthorized = false;

        if (isSuccesfull(status) && StringUtils.isNotEmpty(settlementReference)) {
            isPaymentPaid = true;
        } else if (isSuccesfull(status) && StringUtils.isEmpty(settlementReference)){
            isAuthorized = true;
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

    private String buildSignatureFromReturnData(Map<String,String> checkoutParams) {
        TreeMap<String, String> checkoutSignatureParameters = PaytrailSignatureService.filterCheckoutQueryParametersMap(checkoutParams);

        try {
            String calculatedSignature = PaytrailSignatureService.calculateSignature(checkoutSignatureParameters, null, secretKey);
            log.debug("calculatedSignature: " + calculatedSignature);
            return calculatedSignature;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return null;
        }
    }
}

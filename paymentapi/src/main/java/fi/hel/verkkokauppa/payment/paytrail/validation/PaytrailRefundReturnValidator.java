package fi.hel.verkkokauppa.payment.paytrail.validation;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.util.StringUtils;
import fi.hel.verkkokauppa.payment.api.data.refund.RefundReturnDto;
import fi.hel.verkkokauppa.payment.model.refund.RefundPayment;
import fi.hel.verkkokauppa.payment.repository.refund.RefundPaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.constants.RefundStatus;
import org.helsinki.paytrail.service.PaytrailSignatureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;


@Service
@Slf4j
public class PaytrailRefundReturnValidator {

    @Autowired
    private RefundPaymentRepository refundPaymentRepository;

    @Autowired
    private CommonServiceConfigurationClient commonServiceConfigurationClient;

    public boolean validatePaytrailChecksum(Map<String,String> checkoutParams, String merchantId, String signature, String refundId) {
        RefundPayment refund = refundPaymentRepository.findById(refundId).orElseThrow(() -> new CommonApiException(
                HttpStatus.NOT_FOUND,
                new Error("refund-not-found", "refund with id [" + refundId + "] not found")
        ));
        String createdSignature = buildSignatureFromReturnData(checkoutParams, merchantId);
        if (signature.equals(createdSignature)) {
            return true;
        } else {
            log.debug("paytrail refund validation failed, refundId: " + refundId);
            return false;
        }
    }

    public RefundReturnDto validatePaytrailRefundReturnValues(boolean isValid, String status) {
        boolean isRefundPaid = false;
        boolean canRetry = false;

        if (isSuccessful(status)) {
            isRefundPaid = true;
        } else {
            if (!RefundStatus.PENDING.getStatus().equals(status)) {
                canRetry = true;
            }
        }

        return new RefundReturnDto(isValid, isRefundPaid, canRetry);
    }

    private boolean isSuccessful(String status) {
        return RefundStatus.OK.getStatus().equals(status);
    }

    private String buildSignatureFromReturnData(Map<String,String> checkoutParams, String merchantId) {
        TreeMap<String, String> checkoutSignatureParameters = PaytrailSignatureService.filterCheckoutQueryParametersMap(checkoutParams);

        try {
            String calculatedSignature;
            String merchantSecretKey = commonServiceConfigurationClient.getMerchantPaytrailSecretKey(merchantId);
            if (StringUtils.isEmpty(merchantSecretKey)) {
                log.debug("No paytrail secret key found for merchant {}", merchantId);
                return null;
            }
            calculatedSignature = PaytrailSignatureService.calculateSignature(checkoutSignatureParameters, null, merchantSecretKey);

            log.debug("calculatedSignature: " + calculatedSignature);
            return calculatedSignature;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.debug("Failed to create signature from return data", e);
            log.debug(e.getMessage());
            return null;
        }
    }
}

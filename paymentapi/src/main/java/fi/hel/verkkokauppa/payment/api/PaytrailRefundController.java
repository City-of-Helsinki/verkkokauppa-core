package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.payment.api.data.refund.RefundReturnDto;
import fi.hel.verkkokauppa.payment.paytrail.validation.PaytrailRefundReturnValidator;
import fi.hel.verkkokauppa.payment.service.refund.PaytrailRefundPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Slf4j
public class PaytrailRefundController {

    @Autowired
    private PaytrailRefundPaymentService paytrailRefundPaymentService;

    @Autowired
    private PaytrailRefundReturnValidator paytrailRefundReturnValidator;

    @GetMapping("/refund/paytrail/check-refund-callback-url")
    public ResponseEntity<RefundReturnDto> checkRefundReturnUrl(
            @RequestParam(value = "merchantId") String merchantId,
            @RequestParam(value = "signature") String signature,
            @RequestParam(value = "checkout-status") String status,
            @RequestParam(value = "checkout-stamp") String refundId,
            @RequestParam Map<String,String> checkoutParams
    ) {
        try {
            boolean isValid = paytrailRefundReturnValidator.validatePaytrailChecksum(checkoutParams, merchantId, signature, refundId);
            RefundReturnDto refundDto = paytrailRefundReturnValidator.validatePaytrailRefundReturnValues(isValid, status);
            paytrailRefundPaymentService.updateRefundPaymentStatus(refundId, refundDto);
            return ResponseEntity.status(HttpStatus.OK).body(refundDto);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("checking refund return response failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-check-refund-return-response", "failed to check refund return response")
            );
        }
    }
}

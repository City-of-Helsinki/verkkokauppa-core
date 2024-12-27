package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.payment.api.data.refund.RefundRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.refund.RefundReturnDto;
import fi.hel.verkkokauppa.payment.model.refund.RefundPayment;
import fi.hel.verkkokauppa.payment.paytrail.validation.PaytrailRefundReturnValidator;
import fi.hel.verkkokauppa.payment.service.refund.FreeRefundPaymentService;
import fi.hel.verkkokauppa.payment.service.refund.PaytrailRefundPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Slf4j
public class FreeRefundPaymentController {

    private final FreeRefundPaymentService freeRefundPaymentService;

    @Autowired
    public FreeRefundPaymentController(FreeRefundPaymentService paymentService) {
        this.freeRefundPaymentService = paymentService;
    }

    @PostMapping(value = "/refund/free/createFromRefund", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RefundPayment> createRefundPaymentFromRefund(@RequestBody RefundRequestDataDto dto) {
        try {
            RefundPayment refundPayment = freeRefundPaymentService.createFreeRefundPayment(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(refundPayment);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("creating refund payment or paytrail refund call failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-create-refund-payment", "failed to create refund payment")
            );
        }
    }

    @GetMapping("/refund/free/check-refund-callback-url")
    public ResponseEntity<RefundReturnDto> checkRefundReturnUrl(
            @RequestParam(value = "merchantId") String merchantId,
            @RequestParam(value = "signature") String signature,
            @RequestParam(value = "checkout-status") String status,
            @RequestParam(value = "checkout-stamp") String refundId,
            @RequestParam Map<String, String> checkoutParams
    ) {
        try {
            boolean isValid = refundReturnValidator.validatePaytrailChecksum(checkoutParams, merchantId, signature, refundId);
            RefundReturnDto refundDto = refundReturnValidator.validatePaytrailRefundReturnValues(isValid, status);
            refundPaymentService.updateRefundPaymentStatus(refundId, refundDto);
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

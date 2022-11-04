package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentReturnDto;
import fi.hel.verkkokauppa.payment.paytrail.validation.PaytrailPaymentReturnValidator;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import fi.hel.verkkokauppa.payment.service.PaymentPaytrailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Slf4j
public class PaytrailPaymentController {

    @Autowired
    private PaymentPaytrailService paymentPaytrailService;

    @Autowired
    private OnlinePaymentService onlinePaymentService;

    @Autowired
    private PaytrailPaymentReturnValidator paytrailPaymentReturnValidator;

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
            @RequestParam(value = "signature") String signature,
            @RequestParam(value = "checkout-status") String status,
            @RequestParam(value = "checkout-stamp") String paymentId,
            @RequestParam(value = "checkout-settlement-reference", required = false) String settlementReference,
            @RequestParam Map<String,String> checkoutParams
    ) {
        try {
            boolean isValid = paytrailPaymentReturnValidator.validateChecksum(checkoutParams, signature, paymentId);
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
}

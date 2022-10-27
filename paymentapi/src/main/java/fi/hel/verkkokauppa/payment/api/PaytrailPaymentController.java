package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.service.PaymentPaytrailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class PaytrailPaymentController {

    @Autowired
    private PaymentPaytrailService paymentPaytrailService;

    @PostMapping("/payment/paytrail/createFromOrder")
    public ResponseEntity<Payment> createPaymentFromOrder(@RequestBody GetPaymentRequestDataDto dto) {
        try {
            Payment payment = paymentPaytrailService.getPaymentRequestData(dto);
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
}

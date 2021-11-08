package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.payment.model.Payment;
import fi.hel.verkkokauppa.payment.service.OnlinePaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OnlinePaymentAdminController {

    private Logger log = LoggerFactory.getLogger(OnlinePaymentAdminController.class);

    @Autowired
    private OnlinePaymentService service;

    @GetMapping(value = "/payment-admin/online/get", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Payment> getPayment(@RequestParam(value = "orderId") String orderId) {
        try {
            Payment payment = service.getPaymentForOrder(orderId);
            return ResponseEntity.status(HttpStatus.OK).body(payment);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("getting payment failed, orderId: " + orderId, e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-payment", "failed to get payment with order id [" + orderId + "]")
            );
        }
    }
}

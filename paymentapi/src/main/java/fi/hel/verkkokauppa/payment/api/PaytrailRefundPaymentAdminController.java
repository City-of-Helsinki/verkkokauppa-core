package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.payment.model.refund.RefundPayment;
import fi.hel.verkkokauppa.payment.service.refund.PaytrailRefundPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class PaytrailRefundPaymentAdminController {

    private final PaytrailRefundPaymentService refundPaymentService;

    @Autowired
    public PaytrailRefundPaymentAdminController(PaytrailRefundPaymentService onlinePaymentService) {
        this.refundPaymentService = onlinePaymentService;
    }

    @GetMapping(value = "/refund-admin/refund-payment/get", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RefundPayment> getRefundPayment(@RequestParam(value = "orderId") String orderId) {
        try {
            RefundPayment refundPaymentForOrder = refundPaymentService.getRefundPaymentForOrder(orderId);
            if (refundPaymentForOrder == null) {
                log.error("No refund payments found with given orderId. orderId: " + orderId);
                throw new CommonApiException(
                        HttpStatus.NOT_FOUND,
                        new Error("failed-to-get-refund-payment", "failed to get refund payment with order id [" + orderId + "]")
                );
            }
            return ResponseEntity.status(HttpStatus.OK).body(refundPaymentForOrder);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("getting refund payment failed, orderId: " + orderId, e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-refund-payment", "failed to get refund payment with order id [" + orderId + "]")
            );
        }
    }

    @GetMapping(value = "/refund-admin/refund-payment/get/refundId", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RefundPayment>> getRefundPaymentByRefundId(@RequestParam(value = "refundId") String refundId) {
        try {
            List<RefundPayment> refundPaymentForRefundId = refundPaymentService.getRefundPaymentForOrderByRefundId(refundId);
            if (refundPaymentForRefundId == null) {
                log.error("No refund payments found with given refund id. refundId: " + refundId);
                throw new CommonApiException(
                        HttpStatus.NOT_FOUND,
                        new Error("failed-to-get-refund-payment", "failed to get refund payment with refund id [" + refundId + "]")
                );
            }
            return ResponseEntity.status(HttpStatus.OK).body(refundPaymentForRefundId);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("getting refund payment failed, refundId: " + refundId, e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-get-refund-payment-by-refund-id", "failed to get refund payment with refund id [" + refundId + "]")
            );
        }
    }

}

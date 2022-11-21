package fi.hel.verkkokauppa.payment.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.payment.api.data.refund.RefundRequestDataDto;
import fi.hel.verkkokauppa.payment.model.refund.RefundPayment;
import fi.hel.verkkokauppa.payment.service.refund.RefundPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class PaytrailRefundPaymentController {

  RefundPaymentService refundPaymentService;

  @Autowired
  public PaytrailRefundPaymentController(RefundPaymentService onlinePaymentService) {
    this.refundPaymentService = onlinePaymentService;
  }


  @PostMapping(value = "/refund/paytrail/createFromRefund", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<RefundPayment> createRefundPaymentFromRefund(@RequestBody RefundRequestDataDto dto) {
    try {
      RefundPayment refundPayment = refundPaymentService.getPaymentRequestData(dto);
      return ResponseEntity.status(HttpStatus.CREATED).body(refundPayment);
    } catch (CommonApiException cae) {
      throw cae;
    } catch (Exception e) {
      log.error("creating payment or chargerequest failed", e);
      throw new CommonApiException(
              HttpStatus.INTERNAL_SERVER_ERROR,
              new Error("failed-to-create-refund-payment", "failed to create refund payment")
      );
    }
  }
}

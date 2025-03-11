package fi.hel.verkkokauppa.order.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.order.api.data.accounting.CreateRefundAccountingRequestDto;
import fi.hel.verkkokauppa.order.api.data.accounting.RefundAccountingDto;
import fi.hel.verkkokauppa.order.api.data.accounting.RefundItemAccountingDto;
import fi.hel.verkkokauppa.order.model.accounting.RefundAccounting;
import fi.hel.verkkokauppa.order.service.accounting.RefundAccountingService;
import fi.hel.verkkokauppa.order.service.accounting.RefundItemAccountingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RefundAccountingController {

    private Logger log = LoggerFactory.getLogger(RefundAccountingController.class);

    @Autowired
    private RefundAccountingService refundAccountingService;

    @Autowired
    private RefundItemAccountingService refundItemAccountingService;

    @PostMapping(value = "/refund/accounting/create", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RefundAccountingDto> createRefundAccounting(@RequestBody CreateRefundAccountingRequestDto request) {
        try {
            String refundId = request.getRefundId();
            String orderId = request.getOrderId();
            RefundAccounting refundAccounting = refundAccountingService.getRefundAccounting(refundId);

            if (refundAccounting != null) {
                log.info("Accounting for refund already created");
                return ResponseEntity.ok().build();
            }

            List<RefundItemAccountingDto> refundItemAccountings = refundItemAccountingService.createRefundItemAccountings(request);
            RefundAccountingDto refundAccountingDto = refundAccountingService.createRefundAccounting(refundId, orderId, request.getNamespace(), refundItemAccountings);


            return ResponseEntity.ok().body(refundAccountingDto);

        } catch (Exception e) {
            log.error("creating refund accounting failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-create-refund-accounting", "failed to create refund accounting")
            );
        }
    }

}

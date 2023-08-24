package fi.hel.verkkokauppa.order.api.admin;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.SendEventService;
import fi.hel.verkkokauppa.common.events.TopicName;
import fi.hel.verkkokauppa.common.events.message.RefundMessage;
import fi.hel.verkkokauppa.common.history.service.SaveHistoryService;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.common.rest.refund.RefundAggregateDto;
import fi.hel.verkkokauppa.common.rest.refund.RefundDto;
import fi.hel.verkkokauppa.order.api.data.transformer.RefundTransformer;
import fi.hel.verkkokauppa.order.model.refund.Refund;
import fi.hel.verkkokauppa.order.model.refund.RefundItem;
import fi.hel.verkkokauppa.order.model.refund.RefundStatus;
import fi.hel.verkkokauppa.order.repository.jpa.RefundItemRepository;
import fi.hel.verkkokauppa.order.repository.jpa.RefundRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class RefundAdminController {
    private Logger log = LoggerFactory.getLogger(RefundAdminController.class);

    @Autowired
    private RefundRepository refundRepository;
    @Autowired
    private RefundItemRepository refundItemRepository;
    @Autowired
    private RefundTransformer refundTransformer;
    @Autowired
    private SendEventService sendEventService;
    @Autowired
    private SaveHistoryService saveHistoryService;

    private RefundMessage createRefundMessage(String eventType, Refund refund) {
        return RefundMessage.builder()
                .eventType(eventType)
                .namespace(refund.getNamespace())
                .userId(refund.getUser())
                .refundId(refund.getRefundId())
                .orderId(refund.getOrderId())
                .timestamp(DateTimeUtil.getFormattedDateTime(refund.getCreatedAt()))
                .eventTimestamp(DateTimeUtil.getDateTime())
                .build();
    }

    @GetMapping(value = "/refund-admin/get-by-refund-id", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RefundAggregateDto> getRefund(@RequestParam String refundId) {
        Refund refund = refundRepository.findById(refundId).orElseThrow(() -> new CommonApiException(
                HttpStatus.NOT_FOUND,
                new Error("refund-not-found", "refund with id [" + refundId + "] not found")
        ));
        List<RefundItem> refundItems = refundItemRepository.findByRefundId(refund.getRefundId());
        return ResponseEntity.ok().body(refundTransformer.transformToDto(refund, refundItems));
    }

    @GetMapping(value = "/refund-admin/get-by-order-id", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RefundAggregateDto>> getRefundsByOrderId(@RequestParam String orderId) {
        List<Refund> refunds = refundRepository.findByOrderId(orderId);
        List<RefundAggregateDto> dtos = new ArrayList<>();
        for (Refund refund : refunds) {
            List<RefundItem> refundItems = refundItemRepository.findByRefundId(refund.getRefundId());
            dtos.add(refundTransformer.transformToDto(refund, refundItems));
        }
        return ResponseEntity.ok().body(dtos);
    }

    @PostMapping(value = "/refund-admin/confirm", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RefundDto> confirmRefund(@RequestParam String refundId) {
        Refund refund = refundRepository.findById(refundId).orElseThrow(() -> new CommonApiException(
                HttpStatus.NOT_FOUND,
                new Error("refund-not-found", "refund with id [" + refundId + "] not found")
        ));

        if (!refund.getStatus().equals(RefundStatus.DRAFT)) {
            throw new CommonApiException(
                    HttpStatus.BAD_REQUEST,
                    new Error("refund-validation-failed", "refund [" + refundId + "] must be a draft")
            );
        }

        refund.setStatus(RefundStatus.CONFIRMED);
        refundRepository.save(refund);

        RefundMessage refundMessage = createRefundMessage(EventType.REFUND_CONFIRMED, refund);
        sendEventService.sendEventMessage(TopicName.REFUNDS, refundMessage);
        saveHistoryService.saveRefundMessageHistory(refundMessage);

        return ResponseEntity.ok().body(refundTransformer.transformToDto(refund));
    }
}

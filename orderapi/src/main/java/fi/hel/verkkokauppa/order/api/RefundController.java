package fi.hel.verkkokauppa.order.api;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.common.events.EventType;
import fi.hel.verkkokauppa.common.events.SendEventService;
import fi.hel.verkkokauppa.common.events.TopicName;
import fi.hel.verkkokauppa.common.events.message.RefundMessage;
import fi.hel.verkkokauppa.common.history.service.SaveHistoryService;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.data.refund.RefundAggregateDto;
import fi.hel.verkkokauppa.order.api.data.refund.RefundDto;
import fi.hel.verkkokauppa.order.api.data.refund.RefundItemDto;
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
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
public class RefundController {
  private Logger log = LoggerFactory.getLogger(RefundController.class);

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
            .user(refund.getUser())
            .refundId(refund.getRefundId())
            .orderId(refund.getOrderId())
            .timestamp(DateTimeUtil.getFormattedDateTime(refund.getCreatedAt()))
            .build();
  }

  @PostMapping(value = "/refund/create", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<RefundAggregateDto> createRefund(@RequestBody RefundAggregateDto refundAggregateDto) {
    try {
      Refund refund = new Refund(refundAggregateDto.getRefund());
      refundRepository.save(refund);

      List<RefundItem> refundItems = new ArrayList<>();
      for (RefundItemDto refundItemDto : refundAggregateDto.getItems()) {
        RefundItem refundItem = new RefundItem(refund.getRefundId(), refundItemDto);
        refundItemRepository.save(refundItem);
        refundItems.add(refundItem);
      }

      RefundMessage refundMessage = createRefundMessage(EventType.REFUND_CREATED, refund);
      sendEventService.sendEventMessage(TopicName.REFUNDS, refundMessage);
      saveHistoryService.saveRefundMessageHistory(refundMessage);

      return ResponseEntity.ok().body(refundTransformer.transformToDto(refund, refundItems));
    } catch (CommonApiException cae) {
      throw cae;
    } catch (Exception e) {
      log.error("creating refund failed", e);
      throw new CommonApiException(
              HttpStatus.INTERNAL_SERVER_ERROR,
              new Error("failed-to-create-refund", "failed to create refund")
      );
    }
  }
}

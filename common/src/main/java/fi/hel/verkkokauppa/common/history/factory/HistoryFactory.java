package fi.hel.verkkokauppa.common.history.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.events.message.ErrorMessage;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.events.message.RefundMessage;
import fi.hel.verkkokauppa.common.events.message.SubscriptionMessage;
import fi.hel.verkkokauppa.common.history.dto.HistoryDto;
import fi.hel.verkkokauppa.common.history.util.EntityTypeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HistoryFactory {
    @Autowired
    private ObjectMapper objectMapper;

    public HistoryDto fromOrderMessage(OrderMessage message){
        try {
            return HistoryDto.builder()
                    .entityId(message.getOrderId())
                    .entityType(EntityTypeUtil.EVENT)
                    .namespace(message.getNamespace())
                    .user(message.getUserId())
                    .eventType(message.getEventType())
                    .description(message.getOrderType())
                    .payload(objectMapper.writeValueAsString(message))
                    .build();
        } catch (JsonProcessingException e) {
            return HistoryDto.builder()
                    .entityId(message.getOrderId())
                    .entityType(EntityTypeUtil.EVENT)
                    .namespace(message.getNamespace())
                    .user(message.getUserId())
                    .eventType(message.getEventType())
                    .description(message.getOrderType())
                    .payload(e.getMessage())
                    .build();
        }
    }


    public HistoryDto fromPaymentMessage(PaymentMessage message){
        try {
            return HistoryDto.builder()
                    .entityId(message.getOrderId())
                    .entityType(EntityTypeUtil.EVENT)
                    .namespace(message.getNamespace())
                    .user(message.getUserId())
                    .eventType(message.getEventType())
                    .description(message.getOrderType())
                    .payload(objectMapper.writeValueAsString(message))
                    .build();
        } catch (JsonProcessingException e) {
            return HistoryDto.builder()
                    .entityId(message.getOrderId())
                    .namespace(message.getNamespace())
                    .entityType(EntityTypeUtil.EVENT)
                    .user(message.getUserId())
                    .eventType(message.getEventType())
                    .description(message.getOrderType())
                    .payload(e.getMessage())
                    .build();
        }
    }


    public HistoryDto fromSubscriptionMessage(SubscriptionMessage message){
        try {
            return HistoryDto.builder()
                    .entityId(message.getSubscriptionId())
                    .entityType(EntityTypeUtil.EVENT)
                    .namespace(message.getNamespace())
                    .eventType(message.getEventType())
                    .description(message.getCancellationCause())
                    .payload(objectMapper.writeValueAsString(message))
                    .build();
        } catch (JsonProcessingException e) {
            return HistoryDto.builder()
                    .entityId(message.getSubscriptionId())
                    .entityType(EntityTypeUtil.EVENT)
                    .namespace(message.getNamespace())
                    .eventType(message.getEventType())
                    .description(message.getCancellationCause())
                    .payload(e.getMessage())
                    .build();
        }
    }

    public HistoryDto fromRefundMessage(RefundMessage message){
        try {
            return HistoryDto.builder()
                    .entityId(message.getRefundId())
                    .entityType(EntityTypeUtil.EVENT)
                    .namespace(message.getNamespace())
                    .eventType(message.getEventType())
                    .payload(objectMapper.writeValueAsString(message))
                    .build();
        } catch (JsonProcessingException e) {
            return HistoryDto.builder()
                    .entityId(message.getRefundId())
                    .entityType(EntityTypeUtil.EVENT)
                    .namespace(message.getNamespace())
                    .eventType(message.getEventType())
                    .payload(e.getMessage())
                    .build();
        }
    }

    public HistoryDto fromErrorMessage(ErrorMessage message){
        try {
            return HistoryDto.builder()
                    .entityId("-")
                    .namespace("-")
                    .entityType(EntityTypeUtil.ERROR_EVENT)
                    .eventType(message.getEventType())
                    .payload(objectMapper.writeValueAsString(message))
                    .build();
        } catch (JsonProcessingException e) {
            return HistoryDto.builder()
                    .entityId("-")
                    .namespace("-")
                    .entityType(EntityTypeUtil.ERROR_EVENT)
                    .eventType(message.getEventType())
                    .payload(e.getMessage())
                    .build();
        }
    }
}

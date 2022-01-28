package fi.hel.verkkokauppa.common.history.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.events.message.OrderMessage;
import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import fi.hel.verkkokauppa.common.history.dto.HistoryDto;
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
                    .user(message.getUserId())
                    .eventType(message.getEventType())
                    .description(message.getOrderType())
                    .payload(e.getMessage())
                    .build();
        }
    }
}

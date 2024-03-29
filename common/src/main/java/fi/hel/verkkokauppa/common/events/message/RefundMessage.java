package fi.hel.verkkokauppa.common.events.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_NULL)
public class RefundMessage implements EventMessage {
    private String eventType;
    private String eventTimestamp;
    private String namespace;
    private String userId;
    private String refundId;
    private String refundPaymentId;
    private String orderId;
    private String timestamp;
    private String refundType;

    public RefundMessage toCustomerWebHook(){
        return RefundMessage.builder()
                .eventType(this.eventType)
                .timestamp(this.timestamp)
                .refundPaymentId(this.refundPaymentId)
                .orderId(this.orderId)
                .refundId(this.refundId)
                .namespace(this.namespace)
                .eventTimestamp(this.eventTimestamp)
                .build();
    }
}

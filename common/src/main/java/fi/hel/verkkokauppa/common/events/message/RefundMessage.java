package fi.hel.verkkokauppa.common.events.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefundMessage implements EventMessage {
    private String eventType;
    private String namespace;
    private String userId;

    private String refundId;
    private String paymentId;
    private String orderId;
    private String timestamp;

    public RefundMessage toCustomerWebHook(){
        return RefundMessage.builder()
                .eventType(this.eventType)
                .timestamp(this.timestamp)
                .paymentId(this.paymentId)
                .orderId(this.orderId)
                .refundId(this.refundId)
                .namespace(this.namespace)
                .build();
    }
}

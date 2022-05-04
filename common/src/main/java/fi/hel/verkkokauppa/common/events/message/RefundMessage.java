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
    String eventType;
    String namespace;
    String user;

    String refundId;
    String orderId;
    String timestamp;
}

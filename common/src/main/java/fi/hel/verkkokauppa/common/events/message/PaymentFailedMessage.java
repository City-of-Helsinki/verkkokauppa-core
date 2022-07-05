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
public class PaymentFailedMessage implements EventMessage {
    public String eventType;
    public String eventTimestamp;
    public String namespace;
    public String paymentId;

    public PaymentFailedMessage toCustomerWebHook() {
        return PaymentFailedMessage.builder()
                .eventType(this.getEventType())
                .eventTimestamp(this.getEventTimestamp())
                .paymentId(this.getPaymentId())
                .namespace(this.getNamespace())
                .build();
    }
}

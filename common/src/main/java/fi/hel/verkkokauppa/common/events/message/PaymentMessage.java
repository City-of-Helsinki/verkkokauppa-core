package fi.hel.verkkokauppa.common.events.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentMessage implements EventMessage {
    public String eventType;
    public String eventTimestamp;
    public String namespace;

    public String paymentId;
    public String orderId;
    public String userId;
    public String paymentPaidTimestamp;
    public String orderType;

    public String encryptedCardToken;
    public Short cardTokenExpYear;
    public Byte cardTokenExpMonth;

}

package fi.hel.verkkokauppa.common.events.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import fi.hel.verkkokauppa.common.constants.PaymentGatewayEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(value = JsonInclude.Include.NON_EMPTY, content = JsonInclude.Include.NON_NULL)
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
    public String cardLastFourDigits;

    public PaymentGatewayEnum paymentGateway;

    public PaymentMessage toCustomerWebHook(){
        return PaymentMessage.builder()
                .eventType(this.getEventType())
                .eventTimestamp(this.getEventTimestamp())
                .paymentId(this.getPaymentId())
                .orderId(this.getOrderId())
                .namespace(this.getNamespace())
                .paymentPaidTimestamp(this.getPaymentPaidTimestamp())
                .build();
    }

}

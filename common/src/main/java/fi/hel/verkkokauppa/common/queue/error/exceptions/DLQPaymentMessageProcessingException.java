package fi.hel.verkkokauppa.common.queue.error.exceptions;

import fi.hel.verkkokauppa.common.events.message.PaymentMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public class DLQPaymentMessageProcessingException extends RuntimeException {
    private PaymentMessage paymentMessage;
    public DLQPaymentMessageProcessingException(String message, PaymentMessage paymentMessage) {
        super(message);
        this.paymentMessage = paymentMessage;
        log.error(message + paymentMessage.toString());
    }
}

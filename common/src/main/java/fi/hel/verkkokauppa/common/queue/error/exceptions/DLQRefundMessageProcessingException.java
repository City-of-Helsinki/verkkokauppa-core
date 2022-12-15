package fi.hel.verkkokauppa.common.queue.error.exceptions;

import fi.hel.verkkokauppa.common.events.message.RefundMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public class DLQRefundMessageProcessingException extends RuntimeException {
    private RefundMessage refundMessage;
    public DLQRefundMessageProcessingException(String message, RefundMessage refundMessage) {
        super(message);
        this.refundMessage = refundMessage;
        log.error(message + refundMessage.toString());
    }
}

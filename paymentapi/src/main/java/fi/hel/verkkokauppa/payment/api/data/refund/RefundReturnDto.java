package fi.hel.verkkokauppa.payment.api.data.refund;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundReturnDto {
    private boolean isValid;        // checksum ok
    private boolean isRefundPaid;   // status values confirm refund
    private boolean canRetry;       // not all failures can be retried
    private String paymentType;     // PaymentType constant.

    public RefundReturnDto() {
    }

    public RefundReturnDto(boolean isValid, boolean isRefundPaid, boolean canRetry) {
        this.isValid = isValid;
        this.isRefundPaid = isRefundPaid;
        this.canRetry = canRetry;
    }
}

package fi.hel.verkkokauppa.payment.api.data.refund;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RefundReturnDto {
    private boolean isValid;        // checksum ok
    private boolean isRefundPaid;   // status values confirm refund
    private boolean canRetry;       // not all failures can be retried

    public RefundReturnDto(boolean isValid, boolean isRefundPaid, boolean canRetry) {
        this.isValid = isValid;
        this.isRefundPaid = isRefundPaid;
        this.canRetry = canRetry;
    }
}

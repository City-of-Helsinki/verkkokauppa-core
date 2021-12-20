package fi.hel.verkkokauppa.payment.api.data;

public class PaymentReturnDto {
    private boolean isValid;        // checksum ok
    private boolean isAuthorized;   // payment only authorized.
    private boolean isPaymentPaid;  // status values confirm paid
    private boolean canRetry;       // not all failures can be retried
    
    public PaymentReturnDto() {
    }

    public PaymentReturnDto(boolean isValid, boolean isPaymentPaid, boolean canRetry, boolean isAuthorized) {
        this.isValid = isValid;
        this.isPaymentPaid = isPaymentPaid;
        this.canRetry = canRetry;
        this.isAuthorized = isAuthorized;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean isValid) {
        this.isValid = isValid;
    }

    public boolean isPaymentPaid() {
        return isPaymentPaid;
    }

    public void setPaymentPaid(boolean isPaymentPaid) {
        this.isPaymentPaid = isPaymentPaid;
    }

    public boolean isCanRetry() {
        return canRetry;
    }

    public void setCanRetry(boolean canRetry) {
        this.canRetry = canRetry;
    }

}

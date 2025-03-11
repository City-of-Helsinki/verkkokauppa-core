package fi.hel.verkkokauppa.payment.testing.constants;

public enum RefundAccountingStatusEnum {
    CREATED("created"),
    EXPORTED("exported");

    private final String status;

    RefundAccountingStatusEnum(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return status;
    }
}
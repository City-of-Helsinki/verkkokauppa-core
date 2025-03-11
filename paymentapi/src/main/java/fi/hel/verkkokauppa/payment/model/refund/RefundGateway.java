package fi.hel.verkkokauppa.payment.model.refund;

import com.fasterxml.jackson.annotation.JsonValue;


public enum RefundGateway {
    PAYTRAIL("online-paytrail"),
    FREE("free");

    @JsonValue
    private final String type;

    RefundGateway(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return type;
    }
}
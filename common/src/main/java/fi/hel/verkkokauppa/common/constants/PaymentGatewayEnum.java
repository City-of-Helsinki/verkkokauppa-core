package fi.hel.verkkokauppa.common.constants;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PaymentGatewayEnum {
    VISMA("online"),
    PAYTRAIL("online-paytrail"),
    INVOICE("offline");

    @JsonValue
    private final String type;

    PaymentGatewayEnum(String type) {
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

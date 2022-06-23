package fi.hel.verkkokauppa.payment.constant;

import com.fasterxml.jackson.annotation.JsonValue;

public enum GatewayEnum {
    ONLINE("online"),
    OFFLINE("offline");

    @JsonValue
    private final String type;

    GatewayEnum(String type) {
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

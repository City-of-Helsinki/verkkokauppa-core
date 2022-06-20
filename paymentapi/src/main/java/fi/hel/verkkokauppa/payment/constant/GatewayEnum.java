package fi.hel.verkkokauppa.payment.constant;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

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

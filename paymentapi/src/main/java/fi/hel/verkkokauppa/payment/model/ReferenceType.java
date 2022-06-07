package fi.hel.verkkokauppa.payment.model;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ReferenceType {
    ORDER("order"),
    MERCHANT("merchant");

    private final String value;

    private ReferenceType(String value) {
        this.value = value;
    }

    public ReferenceType fromValue(String label) {
        return Arrays.stream(values()).filter(val -> val.value.equals(label)).findFirst().orElse(null);
    }

}

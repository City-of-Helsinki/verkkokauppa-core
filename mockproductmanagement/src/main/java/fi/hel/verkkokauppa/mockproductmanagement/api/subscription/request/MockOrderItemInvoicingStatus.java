package fi.hel.verkkokauppa.mockproductmanagement.api.subscription.request;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MockOrderItemInvoicingStatus {

    CREATED ("created"),
    CANCELLED ("cancelled"),

    INVOICED("invoiced");

    @JsonValue
    private final String status;

    MockOrderItemInvoicingStatus(String status) {
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

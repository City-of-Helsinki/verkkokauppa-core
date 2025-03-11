package fi.hel.verkkokauppa.order.model.invoice;

import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderItemInvoicingStatus {

    CREATED ("created"),
    CANCELLED ("cancelled"),

    INVOICED("invoiced");

    @JsonValue
    private final String status;

    OrderItemInvoicingStatus(String status) {
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

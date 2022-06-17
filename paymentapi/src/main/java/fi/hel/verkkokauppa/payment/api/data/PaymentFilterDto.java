package fi.hel.verkkokauppa.payment.api.data;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentFilterDto {
    String filterId;
    LocalDateTime createdAt;
    String namespace;
    String referenceId;
    // ReferenceType marks whether the filter is for order or for merchant type payment
    String referenceType;
    String filterType;
    String value;
}

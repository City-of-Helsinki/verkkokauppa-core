package fi.hel.verkkokauppa.common.contracts;

import java.time.Instant;
import java.time.LocalDateTime;

public interface OrderItemSubscriptionFields {
    Long getPeriodFrequency();
    String getPeriodUnit();
    Integer getPeriodCount();
    Instant getStartDate();
    LocalDateTime getBillingStartDate();
}

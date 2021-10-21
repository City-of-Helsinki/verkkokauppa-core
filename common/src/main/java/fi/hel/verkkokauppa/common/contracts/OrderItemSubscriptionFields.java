package fi.hel.verkkokauppa.common.contracts;

import java.time.LocalDateTime;

public interface OrderItemSubscriptionFields {
    Long getPeriodFrequency();
    String getPeriodUnit();
    Integer getPeriodCount();
    LocalDateTime getStartDate();
    LocalDateTime getBillingStartDate();
}

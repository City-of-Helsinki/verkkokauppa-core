package fi.hel.verkkokauppa.order.api.data.subscription;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubscriptionCardExpiredDto {

    private String subscriptionCardExpiredId;

    private String subscriptionId;

    private String namespace;

    private LocalDateTime createdAt;
}

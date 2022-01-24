package fi.hel.verkkokauppa.order.api.request.subscription;

import lombok.Data;

@Data
public class SubscriptionPriceRequest {
    private String subscriptionId;
    private String userId;
    private String namespace;
}

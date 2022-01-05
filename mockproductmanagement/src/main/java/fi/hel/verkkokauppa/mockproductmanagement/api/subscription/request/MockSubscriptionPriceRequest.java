package fi.hel.verkkokauppa.mockproductmanagement.api.subscription.request;

import lombok.Data;

@Data
public class MockSubscriptionPriceRequest {
        private String subscriptionId;
        private String userId;
        private String namespace;
}

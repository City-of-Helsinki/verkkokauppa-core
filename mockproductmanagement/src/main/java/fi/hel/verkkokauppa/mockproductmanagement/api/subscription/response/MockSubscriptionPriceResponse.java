package fi.hel.verkkokauppa.mockproductmanagement.api.subscription.response;

import lombok.Data;

@Data
public class MockSubscriptionPriceResponse {
        private String subscriptionId;
        private String userId;
        private String priceNet;
        private String priceVat;
        private String priceGross;
}

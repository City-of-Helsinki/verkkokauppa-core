package fi.hel.verkkokauppa.mockproductmanagement.api.subscription.request;

import lombok.Data;

@Data
public class MockResolveProductRequest {
    private String subscriptionId;
    private String userId;
    private String namespace;
    private MockOrderItemDto orderItem;
}

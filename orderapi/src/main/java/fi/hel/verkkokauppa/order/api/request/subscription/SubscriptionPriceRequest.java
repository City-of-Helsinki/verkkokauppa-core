package fi.hel.verkkokauppa.order.api.request.subscription;

import fi.hel.verkkokauppa.order.api.data.OrderItemDto;
import lombok.Data;

@Data
public class SubscriptionPriceRequest {
    private String subscriptionId;
    private String userId;
    private String namespace;
    private OrderItemDto orderItem;
}

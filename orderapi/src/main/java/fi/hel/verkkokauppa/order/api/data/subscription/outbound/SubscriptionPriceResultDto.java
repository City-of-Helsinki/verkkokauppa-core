package fi.hel.verkkokauppa.order.api.data.subscription.outbound;

import lombok.Data;

@Data
public class SubscriptionPriceResultDto {
    private String subscriptionId;
    private String userId;
    private String priceNet;
    private String priceVat;
    private String priceGross;
}

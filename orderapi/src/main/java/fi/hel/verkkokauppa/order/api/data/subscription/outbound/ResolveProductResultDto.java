package fi.hel.verkkokauppa.order.api.data.subscription.outbound;

import fi.hel.verkkokauppa.order.api.data.OrderItemMetaDto;
import lombok.Data;

@Data
public class ResolveProductResultDto {
    private String subscriptionId;
    private String userId;
    private String productId;
    private String productName;
    private String productLabel;
    private String productDescription;
    private OrderItemMetaDto[] orderItemMetas;

}

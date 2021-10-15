package fi.hel.verkkokauppa.order.api.data;

import fi.hel.verkkokauppa.common.contracts.SubscriptionItem;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Getter
public class OrderItemDto implements SubscriptionItem {

    private String orderItemId;
    private String orderId;
    private String productId;
    private String productName;
    private Integer quantity;
    private String unit;
    private String rowPriceNet;
    private String rowPriceVat;
    private String rowPriceTotal;
    private LocalDateTime startDate; // TODO: aika myös?
    private LocalDateTime billingStartDate; // TODO: aika myös?
    private String periodUnit;
    private Long periodFrequency;
    private Integer periodCount;
    private String vatPercentage;
    private String priceNet;
    private String priceVat;
    private String priceGross;

    private List<OrderItemMetaDto> meta = new ArrayList<>();

}

package fi.hel.verkkokauppa.order.api.data;

import fi.hel.verkkokauppa.common.contracts.OrderItemSubscriptionFields;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Getter
public class OrderItemDto implements OrderItemSubscriptionFields {

    private String orderItemId;
    private String orderId;
    private String productId;
    private String productName;
    private String unit;
    private Integer quantity;
    private String rowPriceNet;
    private String rowPriceVat;
    private String rowPriceTotal;
    private LocalDateTime startDate; // TODO: aika myös?
    private LocalDateTime billingStartDate; // TODO: aika myös?
    private String vatPercentage;
    private String priceNet;
    private String priceVat;
    private String priceGross;

    private String periodUnit;
    private Long periodFrequency;
    private Integer periodCount;

    private List<OrderItemMetaDto> meta = new ArrayList<>();

}

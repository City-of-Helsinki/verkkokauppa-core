package fi.hel.verkkokauppa.order.api.data;

import fi.hel.verkkokauppa.common.contracts.OrderItemSubscriptionFields;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemDto implements OrderItemSubscriptionFields {

    private String merchantId;
    private String orderItemId;
    private String orderId;
    private String productId;
    private String productName;
    private String productLabel;
    private String productDescription;
    private String unit;
    private Integer quantity;
    private String rowPriceNet;
    private String rowPriceVat;
    private String rowPriceTotal;
    private String vatPercentage;
    private String priceNet;
    private String priceVat;
    private String priceGross;
    private String originalPriceNet;
    private String originalPriceVat;
    private String originalPriceGross;
    // Subscription fields
    private Long periodFrequency;
    private String periodUnit;
    private Integer periodCount;
    private LocalDateTime startDate;
    private LocalDateTime billingStartDate;

    private LocalDate invoicingDate;

    private String invoicingStatus;

    private List<OrderItemMetaDto> meta = new ArrayList<>();

}

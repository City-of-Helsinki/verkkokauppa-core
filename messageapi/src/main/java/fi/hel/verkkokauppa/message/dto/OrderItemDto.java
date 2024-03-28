package fi.hel.verkkokauppa.message.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class OrderItemDto {
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

    private String invoicingIncrementId;

    private List<OrderItemMetaDto> meta = new ArrayList<>();
}

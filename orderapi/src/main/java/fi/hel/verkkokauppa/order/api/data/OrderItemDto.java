package fi.hel.verkkokauppa.order.api.data;

import lombok.Data;

import java.time.LocalDate;

@Data
public class OrderItemDto {

    private String orderItemId;

    private String orderId;

    private String productId;

    private String productName;

    private Integer quantity;

    private String unit;

    private String rowPriceNet;

    private String rowPriceVat;

    private String rowPriceTotal;

    private LocalDate startDate; // TODO: aika myös?

    private String periodUnit;

    private Long periodFrequency;
}

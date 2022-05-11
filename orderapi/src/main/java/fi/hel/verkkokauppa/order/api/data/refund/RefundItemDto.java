package fi.hel.verkkokauppa.order.api.data.refund;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefundItemDto {

    private String refundItemId;
    private String refundId;
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

}

package fi.hel.verkkokauppa.payment.api.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemDto {

	private String orderItemId;
	private String orderId;
	private String merchantId;
	private String productId;
	private String productName;
	private Integer quantity;
	private String unit;
	private BigDecimal rowPriceNet;
	private BigDecimal rowPriceVat;
	private BigDecimal rowPriceTotal;
	private String vatPercentage;
	private BigDecimal priceNet;
	private BigDecimal priceVat;
	private BigDecimal priceGross;
	private String productLabel;
	private String productDescription;

}

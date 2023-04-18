package fi.hel.verkkokauppa.payment.api.data;

import lombok.Data;

import java.math.BigDecimal;

@Data
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

}

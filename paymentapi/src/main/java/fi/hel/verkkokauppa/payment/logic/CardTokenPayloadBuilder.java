package fi.hel.verkkokauppa.payment.logic;

import fi.hel.verkkokauppa.payment.api.data.ChargeCardTokenRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import org.helsinki.vismapay.model.payment.Customer;
import org.helsinki.vismapay.model.payment.Initiator;
import org.helsinki.vismapay.model.payment.Product;
import org.helsinki.vismapay.model.payment.ProductType;
import org.helsinki.vismapay.request.payment.ChargeCardTokenRequest;
import org.helsinki.vismapay.request.payment.ChargeRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

@Component
public class CardTokenPayloadBuilder {

	public static final short PAYMENT_INITIATOR_TYPE_MIT = 1; // Merchant initiated transaction (MIT)

	public ChargeCardTokenRequest.CardTokenPayload buildFor(PaymentContext context, ChargeCardTokenRequestDataDto request ) {
		ChargeCardTokenRequest.CardTokenPayload payload = new ChargeCardTokenRequest.CardTokenPayload();

		String paymentOrderNumber = generatePaymentOrderNumber(request.getOrderId());
		payload.setOrderNumber(paymentOrderNumber);
		payload.setAmount(convertToCents(new BigDecimal(request.getPriceTotal())).toBigInteger());
		payload.setCurrency(context.getDefaultCurrency());
		payload.setCardToken(request.getCardToken());

		Initiator initiator = new Initiator();
		initiator.setType(PAYMENT_INITIATOR_TYPE_MIT);
		payload.setInitiator(initiator);

		assignProduct(payload, context, request);

		return payload;
	}

	private void assignProduct(ChargeCardTokenRequest.CardTokenPayload payload, PaymentContext context, ChargeCardTokenRequestDataDto request) {
		Product product = new Product();
		product.setId(request.getOrderItemId())
				.setType(ProductType.TYPE_PRODUCT)
				.setTitle(request.getProductName())
				.setCount(Integer.valueOf(request.getProductQuantity()))
				.setPretaxPrice(convertToCents(new BigDecimal(request.getPriceNet())))
				.setTax(Integer.valueOf(request.getVatPercentage()))
				.setPrice(convertToCents(new BigDecimal(request.getPriceTotal())))
				.setMerchantId(context.getMerchantId())
				.setCp(context.getCp());

		payload.addProduct(product);
	}

	private BigDecimal convertToCents(BigDecimal input) {
		BigDecimal multiplier = BigDecimal.valueOf(100L);
		return input.multiply(multiplier);
	}

	private String generatePaymentOrderNumber(String orderId) {
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
		String currentMinute = sdf.format(timestamp);

		return orderId + "_at_" + currentMinute;
	}

}

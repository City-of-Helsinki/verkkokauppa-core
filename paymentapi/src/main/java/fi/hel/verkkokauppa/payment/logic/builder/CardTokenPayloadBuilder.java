package fi.hel.verkkokauppa.payment.logic.builder;

import fi.hel.verkkokauppa.payment.api.data.ChargeCardTokenRequestDataDto;
import fi.hel.verkkokauppa.payment.logic.context.PaymentContext;
import fi.hel.verkkokauppa.payment.logic.util.PaymentUtil;
import org.helsinki.vismapay.model.payment.Initiator;
import org.helsinki.vismapay.model.payment.Product;
import org.helsinki.vismapay.model.payment.ProductType;
import org.helsinki.vismapay.request.payment.ChargeCardTokenRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CardTokenPayloadBuilder {

	public ChargeCardTokenRequest.CardTokenPayload buildFor(PaymentContext context, ChargeCardTokenRequestDataDto request ) {
		ChargeCardTokenRequest.CardTokenPayload payload = new ChargeCardTokenRequest.CardTokenPayload();

		payload.setOrderNumber(request.getPaymentId());
		payload.setAmount(PaymentUtil.convertToCents(new BigDecimal(request.getPriceTotal())).toBigInteger());
		payload.setCurrency(context.getDefaultCurrency());
		payload.setCardToken(request.getCardToken());

		Initiator initiator = new Initiator();
		initiator.setType(Initiator.TYPE_MERCHANT_INITIATED);
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
				.setPretaxPrice(PaymentUtil.convertToCents(new BigDecimal(request.getPriceNet())))
				.setTax(Integer.valueOf(request.getVatPercentage()))
				.setPrice(PaymentUtil.convertToCents(new BigDecimal(request.getPriceTotal())))
				.setMerchantId(context.getMerchantId())
				.setCp(context.getCp());

		payload.addProduct(product);
	}

}

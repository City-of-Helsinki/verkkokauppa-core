package fi.hel.verkkokauppa.payment.logic.builder;

import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.logic.context.PaymentContext;
import fi.hel.verkkokauppa.payment.logic.util.PaymentUtil;
import org.helsinki.vismapay.model.payment.Customer;
import org.helsinki.vismapay.model.payment.PaymentMethod;
import org.helsinki.vismapay.model.payment.Product;
import org.helsinki.vismapay.model.payment.ProductType;
import org.helsinki.vismapay.request.payment.ChargeRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PaymentOnlyChargeCardPayloadBuilder {

	public ChargeRequest.PaymentTokenPayload buildFor(GetPaymentRequestDataDto dto, PaymentContext context) {
		ChargeRequest.PaymentTokenPayload payload = new ChargeRequest.PaymentTokenPayload();
		OrderDto order = dto.getOrder().getOrder();
		String paymentOrderNumber = PaymentUtil.generatePaymentOrderNumber(order.getOrderId());

		assignPaymentMethod(payload, dto, context);
		assignCustomer(payload, order);
		assignProducts(payload, context);

		payload.setAmount(PaymentUtil.eurosToBigInteger(1))
				.setOrderNumber(paymentOrderNumber)
				.setCurrency(context.getDefaultCurrency());
		return payload;
	}

	private void assignPaymentMethod(ChargeRequest.PaymentTokenPayload payload, GetPaymentRequestDataDto dto, PaymentContext context) {

		PaymentMethod paymentMethod = new PaymentMethod();
		paymentMethod.setType(PaymentMethod.TYPE_EPAYMENT)
				.setReturnUrl(context.getReturnUrl())
				.setNotifyUrl(context.getNotifyUrl())
				.setLang(dto.getLanguage() != null ? dto.getLanguage() : context.getDefaultLanguage())
				.setRegisterCardToken(true)
				.setOverrideAutoSettlement(1); // 1	Auto settlement is disabled and the payment is only authorized (katevaraus)

		if (dto.getPaymentMethod() != null && !dto.getPaymentMethod().isEmpty()) {
			paymentMethod.setSelected(new String[] { dto.getPaymentMethod() });
		}
		payload.setPaymentMethod(paymentMethod);
	}

	private void assignCustomer(ChargeRequest.PaymentTokenPayload payload, OrderDto order) {
		Customer customer = new Customer();
		customer.setFirstname(order.getCustomerFirstName())
				.setLastname(order.getCustomerLastName())
				.setEmail(order.getCustomerEmail());

		payload.setCustomer(customer);
	}

	private void assignProducts(ChargeRequest.PaymentTokenPayload payload, PaymentContext context) {

		Product product = new Product();
		product.setId(UUID.randomUUID().toString())
				.setType(ProductType.TYPE_PRODUCT)
				.setTitle("card_renewal")
				.setCount(1)
				.setPretaxPrice(PaymentUtil.convertToCents(new BigDecimal(1)))
				.setTax(0)
				.setPrice(PaymentUtil.convertToCents(new BigDecimal(1)))
				.setMerchantId(context.getMerchantId())
				.setCp(context.getCp());

		payload.addProduct(product);

	}

}

package fi.hel.verkkokauppa.payment.logic;

import fi.hel.verkkokauppa.payment.api.data.GetPaymentRequestDataDto;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderItemDto;
import org.helsinki.vismapay.model.payment.Customer;
import org.helsinki.vismapay.model.payment.PaymentMethod;
import org.helsinki.vismapay.model.payment.Product;
import org.helsinki.vismapay.model.payment.ProductType;
import org.helsinki.vismapay.request.payment.ChargeRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;

@Component
public class PaymentTokenPayloadBuilder {

	private static final String DEFAULT_LANGUAGE = "fi";

	public ChargeRequest.PaymentTokenPayload buildFor(GetPaymentRequestDataDto dto) {
		ChargeRequest.PaymentTokenPayload payload = new ChargeRequest.PaymentTokenPayload();
		OrderDto order = dto.getOrder().getOrder();

		assignPaymentMethod(payload, dto);
		assignCustomer(payload, order);
		assignProducts(payload, dto);

		payload.setAmount(calculateTotalsInCents(dto))
				.setOrderNumber(order.getOrderId())
				.setCurrency("EUR");
		return payload;
	}

	private void assignPaymentMethod(ChargeRequest.PaymentTokenPayload payload, GetPaymentRequestDataDto dto) {
		boolean isRecurringOrder = dto.getOrder().getOrder().getType().equals("subscription");

		PaymentMethod paymentMethod = new PaymentMethod();
		paymentMethod.setType(PaymentMethod.TYPE_EPAYMENT)
				.setReturnUrl("http://localhost:8080/") // TODO: replace
				.setNotifyUrl("http://localhost:8080/") // TODO: replace
				.setLang(dto.getLanguage() != null ? dto.getLanguage() : DEFAULT_LANGUAGE)
				.setRegisterCardToken(isRecurringOrder);

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

	private void assignProducts(ChargeRequest.PaymentTokenPayload payload, GetPaymentRequestDataDto dto) {
		for (OrderItemDto item : dto.getOrder().getItems()) {
			Product product = new Product();
			product.setId(item.getProductId())
					.setType(ProductType.TYPE_PRODUCT)
					.setTitle(item.getProductName())
					.setCount(item.getQuantity())
					.setPretaxPrice(item.getRowPriceNet())
					.setTax(item.getRowPriceVat().intValue())
					.setPrice(item.getRowPriceTotal())
					.setMerchantId(12345L) // TODO: replace
					.setCp("cp"); // TODO: replace

			payload.addProduct(product);
		}
	}

	private BigInteger calculateTotalsInCents(GetPaymentRequestDataDto dto) {
		BigDecimal totalSum = BigDecimal.valueOf(0);
		BigDecimal multiplier = BigDecimal.valueOf(100L);

		for (OrderItemDto item : dto.getOrder().getItems()) {
			totalSum = totalSum.add(item.getRowPriceTotal());
		}
		return totalSum.multiply(multiplier).toBigInteger(); // TODO: ok?
	}
}

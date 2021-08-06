package fi.hel.verkkokauppa.payment.service;

import fi.hel.verkkokauppa.payment.api.data.PaymentMethodDto;
import fi.hel.verkkokauppa.payment.logic.PaymentMethodListFetcher;
import org.helsinki.vismapay.model.paymentmethods.PaymentMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class PaymentMethodListService {

	@Autowired
	private PaymentMethodListFetcher paymentMethodListFetcher;

	public PaymentMethodDto[] getPaymentMethodList(String currency) {
		try {
			PaymentMethod[] list = paymentMethodListFetcher.getList(currency);

			return Arrays.stream(list).map(paymentMethod -> new PaymentMethodDto(
					paymentMethod.getName(),
					paymentMethod.getSelectedValue(),
					paymentMethod.getGroup(),
					paymentMethod.getImg()
			)).toArray(PaymentMethodDto[]::new);
		} catch (RuntimeException e) {
			return new PaymentMethodDto[0];
		}
	}
}

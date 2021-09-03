package fi.hel.verkkokauppa.payment.service;

import fi.hel.verkkokauppa.payment.api.data.PaymentMethodDto;
import fi.hel.verkkokauppa.payment.logic.PaymentMethodListFetcher;
import org.helsinki.vismapay.model.paymentmethods.PaymentMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class PaymentMethodListService {

	private Logger log = LoggerFactory.getLogger(PaymentMethodListService.class);

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
			log.warn("getting payment methods failed, currency: " + currency, e);
			return new PaymentMethodDto[0];
		}
	}
}

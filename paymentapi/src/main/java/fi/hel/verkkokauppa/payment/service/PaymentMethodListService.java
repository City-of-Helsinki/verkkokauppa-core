package fi.hel.verkkokauppa.payment.service;

import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.constants.PaymentType;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentMethodListRequest;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentMethodDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentMethodFilter;
import fi.hel.verkkokauppa.payment.logic.fetcher.PaymentMethodListFetcher;
import org.helsinki.vismapay.model.paymentmethods.PaymentMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaymentMethodListService {

	private Logger log = LoggerFactory.getLogger(PaymentMethodListService.class);

	@Autowired
	private PaymentMethodListFetcher paymentMethodListFetcher;

	@Autowired
	private Environment env;

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

	public PaymentMethodDto[] filterPaymentMethodList(GetPaymentMethodListRequest request, PaymentMethodDto[] methods) {
		Set<PaymentMethodDto> filteredMethodsList = new HashSet<>();

		// If namespace has multiple filters, available methods for all of them will be returned
		List<PaymentMethodFilter> filtersForNamespace = getFiltersEnabledForNamespace(request.getNamespace());

		for (PaymentMethodFilter paymentMethodFilter : filtersForNamespace) {
			String filterKey = getFilterKeys(paymentMethodFilter, request);

			HashMap<String, List<String>> filterValues = getFilterValuesMap(paymentMethodFilter);

			for (Map.Entry<String, List<String>> entry : filterValues.entrySet()) {
				String key = entry.getKey();

				if (filterKey.equalsIgnoreCase(key)) {
					List<String> paymentMethodGroups = entry.getValue();

					for (String value : paymentMethodGroups) {
						filteredMethodsList.addAll(Arrays.stream(methods)
								.filter(method -> method.getGroup().equalsIgnoreCase(value))
								.collect(Collectors.toList()));
					}
				}
			}
		}

		if (!filteredMethodsList.isEmpty()) {
			methods = filteredMethodsList.toArray(new PaymentMethodDto[0]);
		}

		return methods;
	}

	private List<PaymentMethodFilter> getFiltersEnabledForNamespace(String namespace) {
		List<PaymentMethodFilter> filtersForNamespace = new ArrayList<>();

		for (PaymentMethodFilter methodFilter : PaymentMethodFilter.getAll()) {
			String orderTypeFilterEnabledList = env.getRequiredProperty("enabled_namespaces.payment_method_filter." + methodFilter);

			if (orderTypeFilterEnabledList.contains(namespace)) {
				filtersForNamespace.add(methodFilter);
			}
		}

		return filtersForNamespace;
	}

	public String getFilterKeys(PaymentMethodFilter filter, GetPaymentMethodListRequest request) {
		OrderDto orderDto = request.getOrderDto();

		if (filter.equals(PaymentMethodFilter.ORDER_TYPE)) {
			return orderDto.getType();
		}

		throw new CommonApiException(
				HttpStatus.INTERNAL_SERVER_ERROR,
				new Error("unknown-payment-method-filter-type", "unknown payment method filter type")
		);
	}

	// namespace can be used as a parameter if different namespaces need to return different values for filter type
	public HashMap<String, List<String>> getFilterValuesMap(PaymentMethodFilter filter) {
		HashMap<String, List<String>> values = new HashMap<>();

		if (filter.equals(PaymentMethodFilter.ORDER_TYPE)) {
			values.put(OrderType.SUBSCRIPTION, Collections.singletonList(PaymentType.CREDIT_CARDS));
		}

		return values;
	}

}

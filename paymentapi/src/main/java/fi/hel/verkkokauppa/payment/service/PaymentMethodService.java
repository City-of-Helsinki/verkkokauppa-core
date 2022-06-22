package fi.hel.verkkokauppa.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.constants.PaymentType;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentMethodListRequest;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentMethodDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentMethodFilter;
import fi.hel.verkkokauppa.payment.constant.GatewayEnum;
import fi.hel.verkkokauppa.payment.repository.PaymentMethodRepository;
import fi.hel.verkkokauppa.payment.util.CurrencyUtil;
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
import java.util.stream.StreamSupport;

@Service
public class PaymentMethodService {

	private Logger log = LoggerFactory.getLogger(PaymentMethodService.class);

	private final PaymentMethodRepository paymentMethodRepository;
	private final PaymentMethodListFetcher paymentMethodListFetcher;
	private final Environment env;
	private final ObjectMapper mapper;

	@Autowired
	PaymentMethodService(PaymentMethodRepository paymentMethodRepository,
						 PaymentMethodListFetcher paymentMethodListFetcher,
						 Environment env, ObjectMapper mapper) {
		this.paymentMethodRepository = paymentMethodRepository;
		this.paymentMethodListFetcher = paymentMethodListFetcher;
		this.env = env;
		this.mapper = mapper;
	}

	public PaymentMethodDto[] getOnlinePaymentMethodList(String currency) {
		try {
			PaymentMethod[] list = paymentMethodListFetcher.getList(currency);


			return Arrays.stream(list).map(paymentMethod -> new PaymentMethodDto(
					paymentMethod.getName(),
					paymentMethod.getSelectedValue(),
					paymentMethod.getGroup(),
					paymentMethod.getImg(),
					GatewayEnum.ONLINE
			)).toArray(PaymentMethodDto[]::new);

		} catch (RuntimeException e) {
			log.warn("getting online payment methods failed, currency: " + currency, e);
			return new PaymentMethodDto[0];
		}
	}


	public PaymentMethodDto[] getOfflinePaymentMethodList(String currency) {
		try {
			if (!isDefaultCurrency(currency)) {
				return new PaymentMethodDto[]{};
			}

			List<fi.hel.verkkokauppa.payment.model.PaymentMethod> paymentMethods = paymentMethodRepository.findByGateway(GatewayEnum.OFFLINE);
			return paymentMethods.stream().map(paymentMethod -> new PaymentMethodDto(
					paymentMethod.getName(),
					paymentMethod.getCode(),
					paymentMethod.getGroup(),
					paymentMethod.getImg(),
					paymentMethod.getGateway()
			)).toArray(PaymentMethodDto[]::new);

		} catch (RuntimeException e) {
			log.warn("getting offline payment methods failed, currency: " + currency, e);
			return new PaymentMethodDto[0];
		}
	}

	public boolean isDefaultCurrency(String currency) {
		return Objects.equals(currency, CurrencyUtil.DEFAULT_CURRENCY);
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

	public List<PaymentMethodDto> getAllPaymentMethods() {
		Iterable<fi.hel.verkkokauppa.payment.model.PaymentMethod> paymentMethods = paymentMethodRepository.findAll();

		List<PaymentMethodDto> paymentMethodDtos = StreamSupport.stream(paymentMethods.spliterator(), false)
				.map(paymentMethod -> mapper.convertValue(paymentMethod, PaymentMethodDto.class))
				.collect(Collectors.toList());
		return paymentMethodDtos;
	}

	public PaymentMethodDto getPaymenMethodByCode(String code) {
		fi.hel.verkkokauppa.payment.model.PaymentMethod exsistingMethod = paymentMethodRepository.findByCode(code).stream()
				.filter(paymentMethod -> paymentMethod.getCode().equals(code))
				.findFirst()
				.orElseThrow(() -> new CommonApiException(
						HttpStatus.NOT_FOUND,
						new Error("payment-method-not-found", "payment method with code [" + code + "] not found")
				));
		PaymentMethodDto dto = mapper.convertValue(exsistingMethod, PaymentMethodDto.class);
		return dto;
	}

	public PaymentMethodDto createNewPaymentMethod(PaymentMethodDto dto) {
		paymentMethodRepository.findByCode(dto.getCode()).stream()
				.filter(paymentMethod -> paymentMethod.getCode().equals(dto.getCode()))
				.findFirst()
				.ifPresent(paymentMethod -> {
					throw new CommonApiException(
							HttpStatus.CONFLICT,
							new Error("payment-method-already-exists", "payment method with code [" + paymentMethod.getCode() + "] already exists")
					);
				});

		fi.hel.verkkokauppa.payment.model.PaymentMethod paymentMethod = mapper.convertValue(dto, fi.hel.verkkokauppa.payment.model.PaymentMethod.class);
		fi.hel.verkkokauppa.payment.model.PaymentMethod saved = paymentMethodRepository.save(paymentMethod);

		return new PaymentMethodDto(saved.getName(), saved.getCode(), saved.getGroup(), saved.getImg(), saved.getGateway());
	}

	public PaymentMethodDto updatePaymentMethod(String code, PaymentMethodDto dto) {
		fi.hel.verkkokauppa.payment.model.PaymentMethod paymentMethodToUpdate  = paymentMethodRepository.findByCode(code).stream()
				.filter(paymentMethod -> paymentMethod.getCode().equals(code))
				.findFirst()
				.orElseThrow(() -> new CommonApiException(
						HttpStatus.NOT_FOUND,
						new Error("payment-method-not-found", "payment method with code [" + code + "] not found")
				));
		paymentMethodToUpdate.setName(dto.getName());
		paymentMethodToUpdate.setCode(dto.getCode());
		paymentMethodToUpdate.setGroup(dto.getGroup());
		paymentMethodToUpdate.setImg(dto.getImg());
		paymentMethodToUpdate.setGateway(dto.getGateway());

		fi.hel.verkkokauppa.payment.model.PaymentMethod saved = paymentMethodRepository.save(paymentMethodToUpdate);

		return new PaymentMethodDto(saved.getName(), saved.getCode(), saved.getGroup(), saved.getImg(), saved.getGateway());
	}

	public boolean deletePaymentMethod(String code) {
		fi.hel.verkkokauppa.payment.model.PaymentMethod exsistingMethod = paymentMethodRepository.findByCode(code).stream()
				.filter(paymentMethod -> paymentMethod.getCode().equals(code))
				.findFirst()
				.orElseThrow(() -> new CommonApiException(
						HttpStatus.NOT_FOUND,
						new Error("payment-method-not-found", "payment method with code [" + code + "] not found")
				));
		paymentMethodRepository.delete(exsistingMethod);
		return true;
	}

}

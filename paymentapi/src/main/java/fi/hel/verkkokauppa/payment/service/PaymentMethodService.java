package fi.hel.verkkokauppa.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import fi.hel.verkkokauppa.common.constants.OrderType;
import fi.hel.verkkokauppa.common.constants.PaymentType;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.payment.api.data.GetPaymentMethodListRequest;
import fi.hel.verkkokauppa.payment.api.data.OrderDto;
import fi.hel.verkkokauppa.payment.api.data.OrderPaymentMethodDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentMethodDto;
import fi.hel.verkkokauppa.payment.api.data.PaymentMethodFilter;
import fi.hel.verkkokauppa.payment.constant.PaymentGatewayEnum;
import fi.hel.verkkokauppa.payment.mapper.OrderPaymentMethodMapper;
import fi.hel.verkkokauppa.payment.mapper.PaymentMethodMapper;
import fi.hel.verkkokauppa.payment.model.OrderPaymentMethod;
import fi.hel.verkkokauppa.payment.model.PaymentMethodModel;
import fi.hel.verkkokauppa.payment.repository.OrderPaymentMethodRepository;
import fi.hel.verkkokauppa.payment.repository.PaymentMethodRepository;
import fi.hel.verkkokauppa.payment.util.CurrencyUtil;
import fi.hel.verkkokauppa.payment.logic.fetcher.PaymentMethodListFetcher;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.vismapay.model.paymentmethods.PaymentMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final OrderPaymentMethodRepository orderPaymentMethodRepository;
    private final PaymentMethodListFetcher paymentMethodListFetcher;
    private final Environment env;
    private final PaymentMethodMapper paymentMethodMapper;
    private final OrderPaymentMethodMapper orderPaymentMethodMapper;

    @Autowired
    PaymentMethodService(
            PaymentMethodRepository paymentMethodRepository,
            OrderPaymentMethodRepository orderPaymentMethodRepository,
            PaymentMethodListFetcher paymentMethodListFetcher,
            Environment env,
            PaymentMethodMapper paymentMethodMapper,
            OrderPaymentMethodMapper orderPaymentMethodMapper
    ) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.orderPaymentMethodRepository = orderPaymentMethodRepository;
        this.paymentMethodListFetcher = paymentMethodListFetcher;
        this.env = env;
        this.paymentMethodMapper = paymentMethodMapper;
        this.orderPaymentMethodMapper = orderPaymentMethodMapper;
    }

    public PaymentMethodDto[] getOnlinePaymentMethodList(String currency) {
        try {
            PaymentMethod[] list = paymentMethodListFetcher.getList(currency);


            return Arrays.stream(list).map(paymentMethod -> new PaymentMethodDto(
                    paymentMethod.getName(),
                    paymentMethod.getSelectedValue(),
                    paymentMethod.getGroup(),
                    paymentMethod.getImg(),
                    PaymentGatewayEnum.VISMA
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

            List<PaymentMethodModel> paymentMethodModels = paymentMethodRepository.findByGateway(PaymentGatewayEnum.INVOICE);
            return paymentMethodModels.stream()
                    .map(paymentMethodMapper::toDto)
                    .toArray(PaymentMethodDto[]::new);

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
        Iterable<PaymentMethodModel> paymentMethods = paymentMethodRepository.findAll();

        return StreamSupport.stream(paymentMethods.spliterator(), false)
                .map(paymentMethodMapper::toDto)
                .collect(Collectors.toList());
    }

    public PaymentMethodDto getPaymenMethodByCode(String code) {
        PaymentMethodModel exsistingMethod = paymentMethodRepository.findByCode(code).stream()
                .findFirst()
                .orElseThrow(() -> new CommonApiException(
                        HttpStatus.NOT_FOUND,
                        new Error("payment-method-not-found", "payment method with code [" + code + "] not found")
                ));
        return paymentMethodMapper.toDto(exsistingMethod);
    }

    public PaymentMethodDto createNewPaymentMethod(PaymentMethodDto dto) {
        paymentMethodRepository.findByCode(dto.getCode()).stream()
                .findFirst()
                .ifPresent(paymentMethodModel -> {
                    throw new CommonApiException(
                            HttpStatus.CONFLICT,
                            new Error("payment-method-already-exists", "payment method with code [" + paymentMethodModel.getCode() + "] already exists")
                    );
                });

        PaymentMethodModel paymentMethodModel = paymentMethodMapper.fromDto(dto);
        PaymentMethodModel saved = paymentMethodRepository.save(paymentMethodModel);

        return paymentMethodMapper.toDto(saved);
    }

    public PaymentMethodDto updatePaymentMethod(String code, PaymentMethodDto dto) {
        PaymentMethodModel paymentMethodModelToUpdate = paymentMethodRepository.findByCode(code).stream()
                .findFirst()
                .orElseThrow(() -> new CommonApiException(
                        HttpStatus.NOT_FOUND,
                        new Error("payment-method-not-found", "payment method with code [" + code + "] not found")
                ));
        try {
            paymentMethodModelToUpdate = paymentMethodMapper.updateFromDtoToModel(paymentMethodModelToUpdate, dto);
        } catch (JsonProcessingException e) {
            throw new CommonApiException(
                    HttpStatus.NOT_FOUND,
                    new Error("payment-method-dto-json-error", "payment method with code [" + dto.getCode() + "] json processing error")
            );
        }
        PaymentMethodModel saved = paymentMethodRepository.save(paymentMethodModelToUpdate);

        return paymentMethodMapper.toDto(saved);
    }

    public void deletePaymentMethod(String code) {
        PaymentMethodModel exsistingMethod = paymentMethodRepository.findByCode(code).stream()
                .findFirst()
                .orElseThrow(() -> new CommonApiException(
                        HttpStatus.NOT_FOUND,
                        new Error("payment-method-not-found", "payment method with code [" + code + "] not found")
                ));
        paymentMethodRepository.delete(exsistingMethod);
    }

    public OrderPaymentMethodDto upsertOrderPaymentMethod(OrderPaymentMethodDto dto) {
        Optional<OrderPaymentMethod> orderPaymentMethodOpt = orderPaymentMethodRepository.findByOrderId(dto.getOrderId()).stream().findFirst();
        OrderPaymentMethod paymentMethodToSave;
        if (orderPaymentMethodOpt.isPresent()) {
            try {
                paymentMethodToSave = orderPaymentMethodMapper.updateFromDtoToModel(orderPaymentMethodOpt.get(), dto);
            } catch (JsonProcessingException e) {
                throw new CommonApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        new Error("failed-to-set-order-payment-method-properties", "failed to set order payment method properties")
                );
            }
        } else {
            paymentMethodToSave = orderPaymentMethodMapper.fromDto(dto);
        }
        OrderPaymentMethod saved = orderPaymentMethodRepository.save(paymentMethodToSave);

        return orderPaymentMethodMapper.toDto(saved);
    }

}

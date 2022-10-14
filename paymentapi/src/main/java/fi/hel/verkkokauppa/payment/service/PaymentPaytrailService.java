package fi.hel.verkkokauppa.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.payment.api.data.PaymentMethodDto;
import fi.hel.verkkokauppa.payment.constant.GatewayEnum;
import fi.hel.verkkokauppa.payment.logic.builder.PaytrailPaymentContextBuilder;
import fi.hel.verkkokauppa.payment.logic.context.PaytrailPaymentContext;
import fi.hel.verkkokauppa.payment.logic.fetcher.PaymentMethodListFetcher;
import fi.hel.verkkokauppa.payment.mapper.PaymentMethodMapper;
import fi.hel.verkkokauppa.payment.paytrail.factory.PaytrailAuthClientFactory;
import fi.hel.verkkokauppa.payment.repository.PaymentMethodRepository;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.PaytrailClient;
import org.helsinki.paytrail.mapper.PaytrailPaymentMethodsResponseMapper;
import org.helsinki.paytrail.model.paymentmethods.PaytrailPaymentMethod;
import org.helsinki.paytrail.request.paymentmethods.PaytrailPaymentMethodsRequest;
import org.helsinki.paytrail.response.paymentmethods.PaytrailPaymentMethodsResponse;
import org.helsinki.vismapay.model.paymentmethods.PaymentMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class PaymentPaytrailService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final PaytrailAuthClientFactory paytrailClientFactory;
    private final Environment env;
    private final PaymentMethodMapper paymentMethodMapper;
    private final PaytrailPaymentContextBuilder paymentContextBuilder;
    private final CommonServiceConfigurationClient commonServiceConfigurationClient;
    private final PaytrailPaymentMethodsResponseMapper paymentMethodsResponseMapper;

    @Autowired
    PaymentPaytrailService(
            PaymentMethodRepository paymentMethodRepository,
            PaytrailAuthClientFactory paytrailClientFactory,
            Environment env,
            PaymentMethodMapper paymentMethodMapper,
            PaytrailPaymentContextBuilder paymentContextBuilder,
            CommonServiceConfigurationClient commonServiceConfigurationClient,
            ObjectMapper mapper
    ) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.paytrailClientFactory = paytrailClientFactory;
        this.env = env;
        this.paymentMethodMapper = paymentMethodMapper;
        this.paymentContextBuilder = paymentContextBuilder;
        this.commonServiceConfigurationClient = commonServiceConfigurationClient;
        this.paymentMethodsResponseMapper = new PaytrailPaymentMethodsResponseMapper(mapper);
    }

    public PaymentMethodDto[] getOnlinePaymentMethodList(String merchantId, String namespace, String currency) {
        if (merchantId != null && !merchantId.isEmpty()) {
            String shopId = commonServiceConfigurationClient.getMerchantConfigurationValue(merchantId, namespace, ServiceConfigurationKeys.MERCHANT_SHOP_ID);
            if (shopId != null && !shopId.isEmpty()) {
                PaytrailClient paytrailClient = paytrailClientFactory.getClient(shopId);
                try {
                    PaytrailPaymentMethodsRequest.PaymentMethodsPayload payload = new PaytrailPaymentMethodsRequest.PaymentMethodsPayload();
                    PaytrailPaymentMethodsRequest request = new PaytrailPaymentMethodsRequest(payload);
                    CompletableFuture<PaytrailPaymentMethodsResponse> response = paytrailClient.sendRequest(request);
                    PaytrailPaymentMethodsResponse methodsResponse = paymentMethodsResponseMapper.to(response.get());

                    return methodsResponse.getPaymentMethods().stream().map(paymentMethod -> new PaymentMethodDto(
                            paymentMethod.getName(),
                            paymentMethod.getId(),
                            paymentMethod.getGroup(),
                            paymentMethod.getIcon(),
                            GatewayEnum.ONLINE_PAYTRAIL
                    )).toArray(PaymentMethodDto[]::new);

                } catch (ExecutionException | InterruptedException | RuntimeException e) {
                    log.warn("getting online paytrail payment methods failed, currency: " + currency, e);
                    return new PaymentMethodDto[0];
                }
            } else {
                log.debug("shopId is not cannot be null or empty!");
                return new PaymentMethodDto[0];
            }
        } else {
            log.debug("merchantId cannot be null or empty!");
            return new PaymentMethodDto[0];
        }
    }

}

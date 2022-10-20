package fi.hel.verkkokauppa.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.util.StringUtils;
import fi.hel.verkkokauppa.payment.api.data.PaymentMethodDto;
import fi.hel.verkkokauppa.payment.constant.GatewayEnum;
import fi.hel.verkkokauppa.payment.paytrail.factory.PaytrailAuthClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.helsinki.paytrail.PaytrailClient;
import org.helsinki.paytrail.mapper.PaytrailPaymentMethodsResponseMapper;
import org.helsinki.paytrail.request.paymentmethods.PaytrailPaymentMethodsRequest;
import org.helsinki.paytrail.response.paymentmethods.PaytrailPaymentMethodsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class PaymentPaytrailService {

    private final PaytrailAuthClientFactory paytrailClientFactory;
    private final CommonServiceConfigurationClient commonServiceConfigurationClient;
    private final PaytrailPaymentMethodsResponseMapper paymentMethodsResponseMapper;

    @Autowired
    PaymentPaytrailService(
            PaytrailAuthClientFactory paytrailClientFactory,
            CommonServiceConfigurationClient commonServiceConfigurationClient,
            ObjectMapper mapper
    ) {
        this.paytrailClientFactory = paytrailClientFactory;
        this.commonServiceConfigurationClient = commonServiceConfigurationClient;
        this.paymentMethodsResponseMapper = new PaytrailPaymentMethodsResponseMapper(mapper);
    }

    public PaymentMethodDto[] getOnlinePaymentMethodList(String merchantId, String namespace, String currency) {
        if (StringUtils.isNotEmpty(merchantId)) {
            String shopId = commonServiceConfigurationClient.getMerchantConfigurationValue(merchantId, namespace, ServiceConfigurationKeys.MERCHANT_SHOP_ID);
            if (StringUtils.isNotEmpty(shopId)) {
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

package fi.hel.verkkokauppa.common.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class ServiceUrls {
    @Value("${history.service.url:http://history-api:8080}")
    private String historyServiceUrl;

    @Value("${message.service.url:http://message-api:8080}")
    private String messageServiceUrl;

    @Value("${merchant.service.url:http://merchant-api:8080}")
    private String merchantServiceUrl;

    @Value("${payment.service.url:http://payment-api:8080}")
    private String paymentServiceUrl;

    @Value("${order.service.url:http://order-api:8080}")
    private String orderServiceUrl;

    @Value("${product.service.url:http://product-api:8080}")
    private String productServiceUrl;

    @Value("${productmapping.service.url:http://product-mapping-api:8080/productmapping/}")
    private String productMappingServiceUrl;

    @Value("${serviceconfiguration.url:http://product-mapping-api:8080/productmapping/}")
    private String serviceconfigurationServiceUrl;
}

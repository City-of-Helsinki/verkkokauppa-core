package fi.hel.verkkokauppa.common.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class ExperienceUrls {
    @Value("${order.experience.url:http://order-experience-api:8080/v1/order/}")
    private String orderExperienceUrl;

    @Value("${payment.experience.url:http://payment-experience-api:8080/v1/payment/}")
    private String paymentExperienceUrl;
}

package fi.hel.verkkokauppa.common.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class ExperienceUrls {
    @Value("${order.experience.url}")
    private String orderExperienceUrl;
}

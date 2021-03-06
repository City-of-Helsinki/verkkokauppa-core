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
}

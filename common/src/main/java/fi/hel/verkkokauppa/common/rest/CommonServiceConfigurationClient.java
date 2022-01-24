package fi.hel.verkkokauppa.common.rest;

import lombok.Getter;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;


@Component
@Getter
public class CommonServiceConfigurationClient {
    
    private Logger log = LoggerFactory.getLogger(CommonServiceConfigurationClient.class);

    private final RestServiceClient restServiceClient;

    @Lazy
    @Autowired
    public CommonServiceConfigurationClient(RestServiceClient restServiceClient) {
        this.restServiceClient = restServiceClient;
    }

    @Value("${serviceconfiguration.url:#{null}}")
    private String serviceConfigurationUrl;

    public String getRestrictedServiceConfigurationValue(String namespace, String key) {
        String serviceMappingUrl = serviceConfigurationUrl + "restricted/get?namespace=" + namespace + "&key=" + key;
        JSONObject namespaceServiceConfiguration = restServiceClient.queryJsonService(restServiceClient.getClient(), serviceMappingUrl);
        log.debug("namespaceServiceConfiguration: " + namespaceServiceConfiguration);

        return namespaceServiceConfiguration.optString("configurationValue", null);
    }

    public String getPublicServiceConfigurationValue(String namespace, String key) {
        String serviceMappingUrl = serviceConfigurationUrl + "public/get?namespace=" + namespace + "&key=" + key;
        JSONObject namespaceServiceConfiguration = restServiceClient.queryJsonService(restServiceClient.getClient(), serviceMappingUrl);
        log.debug("namespaceServiceConfiguration: " + namespaceServiceConfiguration);

        return namespaceServiceConfiguration.optString("configurationValue", null);
    }

    public String getAuthKey(String namespace) {
        String serviceMappingUrl = serviceConfigurationUrl + "api-access/get?namespace=" + namespace;

        return restServiceClient.queryStringService(serviceMappingUrl);
    }

}
    

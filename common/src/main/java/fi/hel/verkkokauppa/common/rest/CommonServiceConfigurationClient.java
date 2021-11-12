package fi.hel.verkkokauppa.common.rest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;


@Component
public class CommonServiceConfigurationClient {
    
    private Logger log = LoggerFactory.getLogger(CommonServiceConfigurationClient.class);

    @Autowired
    private RestServiceClient restServiceClient;

    @Value("${serviceconfiguration.url:#{null}}")
    private String serviceConfigurationUrl;

    public String getRestrictedServiceConfigurationValue(String namespace, String key) {
        String serviceMappingUrl = serviceConfigurationUrl + "restricted/get?namespace=" + namespace + "&key=" + key;
        JSONObject namespaceServiceConfiguration = restServiceClient.queryJsonService(restServiceClient.getClient(), serviceMappingUrl);
        log.debug("namespaceServiceConfiguration: " + namespaceServiceConfiguration);

        return namespaceServiceConfiguration.optString("configurationValue", null);
    }

    
}
    

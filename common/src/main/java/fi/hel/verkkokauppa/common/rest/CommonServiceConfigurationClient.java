package fi.hel.verkkokauppa.common.rest;

import fi.hel.verkkokauppa.common.configuration.ServiceUrls;
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

    private final ServiceUrls serviceUrls;
    private Logger log = LoggerFactory.getLogger(CommonServiceConfigurationClient.class);

    private final RestServiceClient restServiceClient;


    @Lazy
    @Autowired
    public CommonServiceConfigurationClient(RestServiceClient restServiceClient, ServiceUrls serviceUrls) {
        this.restServiceClient = restServiceClient;
        this.serviceUrls = serviceUrls;
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

    public String getNamespaceConfigurationValue(String namespace, String key) {
        String merchantApiUrl = serviceUrls.getMerchantServiceUrl() + "/namespace/getValue?namespace=" + namespace + "&key=" + key;
        JSONObject namespaceServiceConfiguration = restServiceClient.queryJsonService(restServiceClient.getClient(), merchantApiUrl);
        log.debug("namespaceConfigurationValue: " + namespaceServiceConfiguration);

        return namespaceServiceConfiguration.optString("value", null);
    }

    public JSONObject getNamespaceModel(String namespace) {
        String merchantApiUrl = serviceUrls.getMerchantServiceUrl() + "/namespace/get?namespace=" + namespace;
        JSONObject namespaceModel = restServiceClient.queryJsonService(restServiceClient.getClient(), merchantApiUrl);
        log.debug("namespaceConfigurationValue: " + namespaceModel);

        return namespaceModel;
    }

    public String getAuthKey(String namespace) {
        String serviceMappingUrl = serviceConfigurationUrl + "api-access/get?namespace=" + namespace;

        return restServiceClient.queryStringService(serviceMappingUrl);
    }

}
    

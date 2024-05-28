package fi.hel.verkkokauppa.common.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceUrls;
import fi.hel.verkkokauppa.common.rest.dto.configuration.MerchantDto;
import fi.hel.verkkokauppa.common.rest.dto.configuration.ServiceConfigurationDto;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@Component
@Getter
public class CommonServiceConfigurationClient {

    private Logger log = LoggerFactory.getLogger(CommonServiceConfigurationClient.class);

    private final RestServiceClient restServiceClient;
    private final ServiceUrls serviceUrls;
    private final ObjectMapper mapper;


    @Lazy
    @Autowired
    public CommonServiceConfigurationClient(RestServiceClient restServiceClient, ServiceUrls serviceUrls, ObjectMapper mapper) {
        this.restServiceClient = restServiceClient;
        this.serviceUrls = serviceUrls;
        this.mapper = mapper;
    }

    public List<ServiceConfigurationDto> getRestrictedServiceConfigurations(String namespace) {
        String serviceMappingUrl = serviceUrls.getServiceconfigurationServiceUrl() + "restricted/getAll?namespace="+namespace;
        JSONArray namespaceServiceConfigurations = restServiceClient.queryJsonArrayService(restServiceClient.getClient(), serviceMappingUrl);
        log.debug("namespaceServiceConfiguration: " + namespaceServiceConfigurations);

        try {
            List<ServiceConfigurationDto> serviceConfigurationDtos = Arrays.asList(mapper.readValue(namespaceServiceConfigurations.toString(), ServiceConfigurationDto[].class));
            return serviceConfigurationDtos;
        } catch (JsonProcessingException e) {
            log.debug(e.toString());
            log.debug("Could not serialize service configurations for namespace: {}", namespace);
            return Collections.emptyList();
        }
    }

    public String getRestrictedServiceConfigurationValue(String namespace, String key) {
        String serviceMappingUrl = serviceUrls.getServiceconfigurationServiceUrl() + "restricted/get?namespace=" + namespace + "&key=" + key;
        JSONObject namespaceServiceConfiguration = restServiceClient.queryJsonService(restServiceClient.getClient(), serviceMappingUrl);
        log.debug("namespaceServiceConfiguration: " + namespaceServiceConfiguration);

        return namespaceServiceConfiguration.optString("configurationValue", null);
    }

    public String getPublicServiceConfigurationValue(String namespace, String key) {
        try {
            String namespaceConfigurationValue = getNamespaceConfigurationValue(namespace, key);
            if (namespaceConfigurationValue != null) {
                return namespaceConfigurationValue;
            }
        } catch (Exception ignored) {
            log.info("No value found from namespace: {} configuration for key: {}", namespace, key);
        }
        String serviceMappingUrl = serviceUrls.getServiceconfigurationServiceUrl() + "public/get?namespace=" + namespace + "&key=" + key;
        JSONObject namespaceServiceConfiguration = restServiceClient.queryJsonService(restServiceClient.getClient(), serviceMappingUrl);
        log.debug("namespaceServiceConfiguration: " + namespaceServiceConfiguration);

        return namespaceServiceConfiguration.optString("configurationValue", null);
    }

    public String getNamespaceConfigurationValue(String namespace, String key) {
        String merchantApiUrl = serviceUrls.getMerchantServiceUrl() + "/namespace/getValue?namespace=" + namespace + "&key=" + key;
        try {
            JSONObject namespaceServiceConfiguration = restServiceClient.queryJsonService(restServiceClient.getClient(), merchantApiUrl);
            log.debug("namespaceConfigurationValue: " + namespaceServiceConfiguration);
            
            return namespaceServiceConfiguration.getJSONObject("configuration").optString("value", null);
        } catch (Exception exception) {
            log.info(exception.toString());
            log.info("No value found from namespace: {} configuration for key: {}", namespace, key);
            return null;
        }
    }

    public JSONObject getNamespaceModel(String namespace) {
        String merchantApiUrl = serviceUrls.getMerchantServiceUrl() + "/namespace/get?namespace=" + namespace;
        try {
            JSONObject namespaceModel = restServiceClient.queryJsonService(restServiceClient.getClient(), merchantApiUrl);
            log.debug("namespaceConfigurationValue: " + namespaceModel);
            return namespaceModel;
        } catch (Exception e) {
            log.info(e.toString());
            log.info("Cant find namespace model with given namespace: {}", namespace);
            return null;
        }
    }

    public MerchantDto getMerchantModel(String merchantId, String namespace) {
        String merchantApiUrl = serviceUrls.getMerchantServiceUrl() + "/merchant/get?merchantId=" + merchantId + "&namespace=" + namespace;
        try {
            JSONObject merchantModel = restServiceClient.queryJsonService(restServiceClient.getClient(), merchantApiUrl);
            log.debug("merchantConfigurationValue: " + merchantModel);

            MerchantDto merchantDto = mapper.readValue(merchantModel.toString(), MerchantDto.class);

            return merchantDto;
        } catch (Exception e) {
            log.debug(e.toString());
            log.debug("Cant find merchant model with given namespace {} and merchantId {}", namespace, merchantId);
            return null;
        }
    }

    public String getMerchantConfigurationValue(String merchantId, String namespace, String key) {
        String merchantApiUrl = serviceUrls.getMerchantServiceUrl() + "/merchant/getValueDto?merchantId=" + merchantId + "&namespace=" + namespace + "&key=" + key;
        try {
            JSONObject merchantServiceConfiguration = restServiceClient.queryJsonService(restServiceClient.getClient(), merchantApiUrl);
            log.debug("merchantConfigurationValue: " + merchantServiceConfiguration);

            return merchantServiceConfiguration.optString("value", null);
        } catch (Exception e) {
            log.debug(e.toString());
            log.debug("Cant find merchant configuration value with given namespace {}, merchantId {} and configuration key {}", namespace, merchantId, key);
            return null;
        }
    }

    public List<MerchantDto> getMerchantsForNamespace(String namespace) {
        String merchantApiUrl = serviceUrls.getMerchantServiceUrl() + "/merchant/list-by-namespace?namespace=" + namespace;
        JSONArray merchantsResponse = restServiceClient.queryJsonArrayService(restServiceClient.getClient(), merchantApiUrl);
        log.info("Merchants: " + merchantsResponse.toString());

        try {
            List<MerchantDto> merchantDtos = Arrays.asList(mapper.readValue(merchantsResponse.toString(), MerchantDto[].class));
            return merchantDtos;
        } catch (Exception e) {
            log.debug(e.toString());
            log.debug("Cant find merchants with given namespace {}", namespace);
            return Collections.emptyList();
        }
    }

    public String getMerchantPaytrailSecretKey(String merchantId) {
        String merchantApiUrl = serviceUrls.getMerchantServiceUrl() + "/merchant/paytrail-secret/get?merchantId=" + merchantId;
        try {
            return restServiceClient.queryStringService(merchantApiUrl);
        } catch (Exception e) {
            log.debug(e.toString());
            log.debug("Cant get secret key for merchant {}", merchantId);
            return null;
        }
    }

    public String getAuthKey(String namespace) {
        String serviceMappingUrl = serviceUrls.getServiceconfigurationServiceUrl() + "api-access/get?namespace=" + namespace;

        return restServiceClient.queryStringService(serviceMappingUrl);
    }

    public String getWebhookAuthKey(String namespace) {
        String serviceMappingUrl = serviceUrls.getServiceconfigurationServiceUrl() + "webhook-api-access/get?namespace=" + namespace;

        return restServiceClient.queryStringService(serviceMappingUrl);
    }


}
    

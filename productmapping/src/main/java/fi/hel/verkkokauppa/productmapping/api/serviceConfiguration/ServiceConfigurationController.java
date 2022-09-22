package fi.hel.verkkokauppa.productmapping.api.serviceConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.util.StringUtils;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.productmapping.model.serviceConfiguration.ServiceConfigurationBatchDto;
import fi.hel.verkkokauppa.productmapping.response.merchant.dto.NamespaceConfigurationDto;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import fi.hel.verkkokauppa.productmapping.model.serviceConfiguration.ServiceConfiguration;

import fi.hel.verkkokauppa.productmapping.service.serviceConfiguration.ServiceConfigurationService;

@RestController
public class ServiceConfigurationController {

    @Autowired
    private Environment env;

    @Autowired
    private ServiceConfigurationService service;

    @Autowired
    private CommonServiceConfigurationClient commonServiceConfigurationClient;

    @Autowired
    private ObjectMapper objectMapper;


    @GetMapping("/serviceconfiguration/keys")
    public ResponseEntity<List<String>> getKeys() {
        List<String> knownKeys = ServiceConfigurationKeys.getAllConfigurationKeys();
        return ResponseEntity.ok(knownKeys);
    }

    /**
     * If key is namespace key, then first find value from merchant-api/namespace/getValue (index namespace)
     * If it is not found then fallback to serviceconfigurations index for value
     */
    @GetMapping("/serviceconfiguration/public/get")
    public ServiceConfiguration getPublicServiceConfiguration(@RequestParam(value = "namespace") String namespace, @RequestParam(value = "key") String key) {
        ServiceConfiguration createdFromNamespaceModel = getNamespaceModelValue(namespace, key);
        if (createdFromNamespaceModel != null) {
            return createdFromNamespaceModel;
        }
        // TODO remove code below when service configuration values are not used anymore
        ServiceConfiguration serviceConfiguration = service.findBy(namespace, key);
        return serviceConfiguration;
    }

    private ServiceConfiguration getNamespaceModelValue(String namespace, String key) {
        if (ServiceConfigurationKeys.getNamespaceKeys().contains(key)) {
            String namespaceModelValue = commonServiceConfigurationClient.getNamespaceConfigurationValue(namespace, key);
            if (StringUtils.isNotEmpty(namespaceModelValue)) {
                ServiceConfiguration createdFromNamespaceModel = new ServiceConfiguration();
                // Mocking configuration key
                createdFromNamespaceModel.setConfigurationId(UUIDGenerator.generateType3UUIDString(namespace, key));
                createdFromNamespaceModel.setNamespace(namespace);
                createdFromNamespaceModel.setConfigurationKey(key);
                createdFromNamespaceModel.setConfigurationValue(namespaceModelValue);
                createdFromNamespaceModel.setRestricted(false);
                return createdFromNamespaceModel;
            }
        }
        return null;
    }

    @GetMapping("/serviceconfiguration/public/getAll")
    public List<ServiceConfiguration> getPublicServiceConfigurationAll(@RequestParam(value = "namespace") String namespace) {
        List<ServiceConfiguration> serviceConfigurations = service.findBy(namespace);
        JSONObject namespaceModelJson = commonServiceConfigurationClient.getNamespaceModel(namespace);
        ArrayList<NamespaceConfigurationDto> configurationDtos;
        ArrayList<ServiceConfiguration> configurationsFromNamespaceDto = null;
        try {
            configurationDtos = new ArrayList<>(
                    objectMapper.readValue(namespaceModelJson.getJSONArray("configurations").toString(),
                            new TypeReference<ArrayList<NamespaceConfigurationDto>>() {
                            }));
            // TODO create override!
            serviceConfigurations.forEach(namespaceConfigurationDto -> {

            });
        } catch (JsonProcessingException e) {
            //throw new RuntimeException(e);
        }



        return serviceConfigurations;
    }

    /**
     * If key is namespace key, then first find value from merchant-api/namespace/getValue (index namespace)
     * If it is not found then fallback to serviceconfigurations index for value
     * TODO access control by path
     */
    @GetMapping("/serviceconfiguration/restricted/get")
    public ServiceConfiguration getRestrictedServiceConfiguration(@RequestParam(value = "namespace") String namespace, @RequestParam(value = "key") String key) {
        ServiceConfiguration createdFromNamespaceModel = getNamespaceModelValue(namespace, key);
        if (createdFromNamespaceModel != null) {
            return createdFromNamespaceModel;
        }
        // TODO remove code below when service configuration values are not used anymore
        ServiceConfiguration serviceConfiguration = service.findRestricted(namespace, key);
        return serviceConfiguration;
    }

    /**
     * TODO access control by path
     */
    @GetMapping("/serviceconfiguration/restricted/getAll")
    public List<ServiceConfiguration> getRestrictedServiceConfigurationAll(@RequestParam(value = "namespace") String namespace) {
        return service.findRestricted(namespace);
    }

    /**
     * Set restricted service configuration
     * TODO access control by path
     */
    @GetMapping("/serviceconfiguration/restricted/create")
    public ResponseEntity<ServiceConfiguration> createRestrictedServiceConfiguration(@RequestParam(value = "namespace") String namespace, @RequestParam(value = "configurationKey") String configurationKey,
                                                                                     @RequestParam(value = "configurationValue") String configurationValue) {
        if (ServiceConfigurationKeys.isRestrictedConfigurationKey(configurationKey)) {
            ServiceConfiguration serviceConfiguration = service.createByParams(namespace, configurationKey, configurationValue, true);
            return ResponseEntity.ok(serviceConfiguration);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Set any publicly readable service configuration
     */
    @GetMapping("/serviceconfiguration/public/create")
    public ResponseEntity<ServiceConfiguration> createPublicServiceConfiguration(@RequestParam(value = "namespace") String namespace, @RequestParam(value = "configurationKey") String configurationKey,
                                                                                 @RequestParam(value = "configurationValue") String configurationValue) {
        if (!ServiceConfigurationKeys.isRestrictedConfigurationKey(configurationKey)) {
            ServiceConfiguration serviceConfiguration = service.createByParams(namespace, configurationKey, configurationValue, false);
            return ResponseEntity.ok(serviceConfiguration);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/serviceconfiguration/public/create-batch")
    public ResponseEntity<List<ServiceConfiguration>> batchCreatePublicServiceConfiguration(@RequestBody ServiceConfigurationBatchDto dto) {
        String namespace = dto.getNamespace();

        dto.getConfigurations().keySet().forEach(configurationKey -> {
            String configurationValue = dto.getConfigurations().get(configurationKey);
            service.createByParams(namespace, configurationKey, configurationValue,
                    ServiceConfigurationKeys.isRestrictedConfigurationKey(configurationKey));
        });

        List<ServiceConfiguration> configurations = service.findBy(namespace);

        return ResponseEntity.ok(configurations);
    }

    @GetMapping("/serviceconfiguration/api-access/create")
    public ResponseEntity<String> createNamespaceAccessToken(@RequestParam(value = "namespace") String namespace) {
        String salt = env.getRequiredProperty("api.access.encryption.salt");
        // TODO fail if empty salt

        String namespaceAccessToken = UUIDGenerator.generateType3UUIDString(namespace, salt);

        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(salt);
        String encryptedNamespaceAccessToken = encryptor.encrypt(namespaceAccessToken);

        service.createByParams(namespace, ServiceConfigurationKeys.NAMESPACE_API_ACCESS_TOKEN, encryptedNamespaceAccessToken, true);

        return ResponseEntity.ok(namespaceAccessToken);
    }

    @GetMapping("/serviceconfiguration/api-access/get")
    public ResponseEntity<String> getNamespaceAccessToken(@RequestParam(value = "namespace") String namespace) {
        String salt = env.getRequiredProperty("api.access.encryption.salt");
        // TODO fail if empty salt

        ServiceConfiguration sc = service.findRestricted(namespace, ServiceConfigurationKeys.NAMESPACE_API_ACCESS_TOKEN);
        String encryptedNamespaceAccessToken = sc.getConfigurationValue();

        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(salt);
        String namespaceAccessToken = encryptor.decrypt(encryptedNamespaceAccessToken);

        return ResponseEntity.ok(namespaceAccessToken);
    }

    @GetMapping("/serviceconfiguration/api-access/validate")
    public ResponseEntity<Boolean> validateNamespaceAccessToken(@RequestParam(value = "namespace") String namespace, @RequestParam(value = "token") String token) {
        String salt = env.getRequiredProperty("api.access.encryption.salt");
        // TODO fail if empty salt

        String whatTheTokenShouldBe = UUIDGenerator.generateType3UUIDString(namespace, salt);

        if (token.equals(whatTheTokenShouldBe)) {
            return ResponseEntity.ok(Boolean.TRUE);
        } else {
            return ResponseEntity.ok(Boolean.FALSE);
        }
    }

    @GetMapping("/serviceconfiguration/initializetestdata")
    public List<ServiceConfiguration> initializeTestData() {
        return service.initializeTestData();
    }


    private ServiceConfigurationBatchDto getConfigurationsDto(String namespace) {
        return new ServiceConfigurationBatchDto(namespace, getConfigurations(namespace));
    }

    private Map<String, String> getConfigurations(String namespace) {
        List<ServiceConfiguration> configurations = service.findBy(namespace);
        Map<String, String> configurationsMap = configurations.stream()
                .collect(Collectors.toMap(ServiceConfiguration::getConfigurationKey, ServiceConfiguration::getConfigurationValue));

        return configurationsMap;
    }

}

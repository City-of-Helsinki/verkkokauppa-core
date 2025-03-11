package fi.hel.verkkokauppa.productmapping.api.serviceConfiguration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.rest.CommonServiceConfigurationClient;
import fi.hel.verkkokauppa.common.util.StringUtils;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.productmapping.model.serviceConfiguration.ServiceConfiguration;
import fi.hel.verkkokauppa.productmapping.model.serviceConfiguration.ServiceConfigurationBatchDto;
import fi.hel.verkkokauppa.productmapping.response.namespace.ConfigurationModel;
import fi.hel.verkkokauppa.productmapping.service.serviceConfiguration.ServiceConfigurationService;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@Slf4j
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

        overrideConfigurationsFromNamespaceModelConfigurations(namespace, serviceConfigurations, namespaceModelJson);

        // contains values from service configurations and namespace, values on namespace model configuration overrides values in service configurations
        return serviceConfigurations;
    }

    private void overrideConfigurationsFromNamespaceModelConfigurations(String namespace, List<ServiceConfiguration> serviceConfigurations, JSONObject namespaceModelJson) {
        if (namespaceModelJson.getJSONArray("configurations") != null) {
            namespaceModelJson.getJSONArray("configurations").forEach(namespaceConfiguration -> {
                try {
                    ConfigurationModel namespaceConfiguration2 = objectMapper.readValue(namespaceConfiguration.toString(), ConfigurationModel.class);
                    // Find first serviceconfiguration using namespace configuration key
                    Optional<ServiceConfiguration> first = serviceConfigurations.stream().filter(
                                    serviceConfiguration -> Objects.equals(
                                            serviceConfiguration.getConfigurationKey(),
                                            namespaceConfiguration2.getKey()
                                    ))
                            .findFirst();

                    // If found, update value from namespace model configuration value
                    first.ifPresent(serviceConfiguration -> serviceConfiguration.setConfigurationValue(namespaceConfiguration2.getValue()));

                    // If not found from service configurations create new entry using namespaceConfiguration key and value
                    if (!first.isPresent()) {
                        serviceConfigurations.add(
                                createFromNamespaceConfiguration(namespace, namespaceConfiguration2)
                        );
                    }
                } catch (JsonProcessingException e) {
                    log.info("Namespace model configuration cant be serialized. Error : {}", e.toString());
                }
            });
        }
    }

    private ServiceConfiguration createFromNamespaceConfiguration(String namespace, ConfigurationModel namespaceConfiguration) {
        ServiceConfiguration e = new ServiceConfiguration();
        String key = namespaceConfiguration.getKey();
        e.setConfigurationId(UUIDGenerator.generateType3UUIDString(namespace, key));
        e.setConfigurationKey(key);
        e.setConfigurationValue(namespaceConfiguration.getValue());
        e.setRestricted(false);
        e.setNamespace(namespace);
        return e;
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
        List<ServiceConfiguration> serviceRestricted = service.findRestricted(namespace);
        JSONObject namespaceModelJson = commonServiceConfigurationClient.getNamespaceModel(namespace);
        overrideConfigurationsFromNamespaceModelConfigurations(namespace, serviceRestricted, namespaceModelJson);

        // contains values from service configurations and namespace, values on namespace model configuration overrides values in service configurations
        return serviceRestricted;
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
        if( sc == null){
            return ResponseEntity.notFound().build();
        }
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

    @GetMapping("/serviceconfiguration/webhook-api-access/create")
    public ResponseEntity<String> createNamespaceWebhookAccessToken(@RequestParam(value = "namespace") String namespace) {
        String salt = env.getRequiredProperty("webhook.access.encryption.salt");
        // TODO fail if empty salt

        String namespaceAccessToken = UUIDGenerator.generateType3UUIDString(namespace, salt);

        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(salt);
        String encryptedNamespaceWebhookAccessToken = encryptor.encrypt(namespaceAccessToken);

        service.createByParams(namespace, ServiceConfigurationKeys.NAMESPACE_WEBHOOK_ACCESS_TOKEN, encryptedNamespaceWebhookAccessToken, true);

        return ResponseEntity.ok(namespaceAccessToken);
    }

    @GetMapping("/serviceconfiguration/webhook-api-access/get")
    public ResponseEntity<String> getNamespaceWebhookAccessToken(@RequestParam(value = "namespace") String namespace) {
        String salt = env.getRequiredProperty("webhook.access.encryption.salt");
        // TODO fail if empty salt

        ServiceConfiguration sc = service.findRestricted(namespace, ServiceConfigurationKeys.NAMESPACE_WEBHOOK_ACCESS_TOKEN);
        String encryptedNamespaceWebhookAccessToken = sc.getConfigurationValue();

        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(salt);
        String namespaceAccessToken = encryptor.decrypt(encryptedNamespaceWebhookAccessToken);

        return ResponseEntity.ok(namespaceAccessToken);
    }

    @GetMapping("/serviceconfiguration/webhook-api-access/validate")
    public ResponseEntity<Boolean> validateNamespaceWebhookAccessToken(@RequestParam(value = "namespace") String namespace, @RequestParam(value = "token") String token) {
        String salt = env.getRequiredProperty("webhook.access.encryption.salt");

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

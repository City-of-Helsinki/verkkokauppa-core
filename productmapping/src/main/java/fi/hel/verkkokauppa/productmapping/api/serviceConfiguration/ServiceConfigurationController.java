package fi.hel.verkkokauppa.productmapping.api.serviceConfiguration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.productmapping.model.serviceConfiguration.ServiceConfigurationBatchDto;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import fi.hel.verkkokauppa.productmapping.model.serviceConfiguration.ServiceConfiguration;
import fi.hel.verkkokauppa.productmapping.model.serviceConfiguration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.productmapping.service.serviceConfiguration.ServiceConfigurationService;

@RestController
public class ServiceConfigurationController {

	@Autowired
	private Environment env;

    @Autowired
    private ServiceConfigurationService service;


	@GetMapping("/serviceconfiguration/keys")
	public ResponseEntity<List<String>> getKeys() {
		List<String> knownKeys = ServiceConfigurationKeys.getAllConfigurationKeys();
		return ResponseEntity.ok(knownKeys);
	}

	@GetMapping("/serviceconfiguration/public/get")
	public ServiceConfiguration getPublicServiceConfiguration(@RequestParam(value = "namespace") String namespace, @RequestParam(value = "key") String key) {
		return service.findBy(namespace, key);
	}

    @GetMapping("/serviceconfiguration/public/getAll")
	public List<ServiceConfiguration> getPublicServiceConfigurationAll(@RequestParam(value = "namespace") String namespace) {
		return service.findBy(namespace);
	}

	/**
	 * TODO access control by path
	 */	
	@GetMapping("/serviceconfiguration/restricted/get")
	public ServiceConfiguration getRestrictedServiceConfiguration(@RequestParam(value = "namespace") String namespace, @RequestParam(value = "key") String key) {
		return service.findRestricted(namespace, key);
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

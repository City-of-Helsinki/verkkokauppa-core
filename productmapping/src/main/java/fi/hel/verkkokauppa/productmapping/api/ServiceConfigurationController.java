package fi.hel.verkkokauppa.productmapping.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import fi.hel.verkkokauppa.productmapping.model.ServiceConfigurationBatchDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import fi.hel.verkkokauppa.productmapping.model.ServiceConfiguration;
import fi.hel.verkkokauppa.productmapping.model.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.productmapping.service.ServiceConfigurationService;

@RestController
public class ServiceConfigurationController {

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

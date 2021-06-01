package fi.hel.verkkokauppa.productmapping.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fi.hel.verkkokauppa.productmapping.model.ServiceConfiguration;
import fi.hel.verkkokauppa.productmapping.service.ServiceConfigurationService;

@RestController
public class ServiceConfigurationController {

    @Autowired
    private ServiceConfigurationService service;

	@GetMapping("/serviceconfiguration/get")
	public ServiceConfiguration getServiceMapping(@RequestParam(value = "n") String namespace, @RequestParam(value = "k") String key) {
		return service.findBy(namespace, key);
	}

    @GetMapping("/serviceconfiguration/getAll")
	public List<ServiceConfiguration> getAllServiceMapping(@RequestParam(value = "n") String namespace) {
		return service.findBy(namespace);
	}

    @GetMapping("/serviceconfiguration/create")
	public ServiceConfiguration createServiceMapping(@RequestParam(value = "namespace") String namespace, @RequestParam(value = "configurationKey") String configurationKey, 
        @RequestParam(value = "configurationValue") String configurationValue) {
		return service.createByParams(namespace, configurationKey, configurationValue);
	}

    @GetMapping("/serviceconfiguration/initializetestdata")
	public List<ServiceConfiguration> initializeTestData() {
        return service.initializeTestData();
	}

}

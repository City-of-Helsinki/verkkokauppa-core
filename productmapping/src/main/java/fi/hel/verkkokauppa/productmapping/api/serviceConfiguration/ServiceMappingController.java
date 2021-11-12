package fi.hel.verkkokauppa.productmapping.api.serviceConfiguration;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fi.hel.verkkokauppa.productmapping.model.serviceConfiguration.ServiceMapping;
import fi.hel.verkkokauppa.productmapping.service.serviceMapping.ServiceMappingService;

@RestController
public class ServiceMappingController {

    @Autowired
    private ServiceMappingService service;

    /**
     * Find the service mapping via namespace and service type. For example asukaspysakointi and price.
     * 
     * @param namespace
     * @param type
     * @return ServiceMapping with backend service url
     */
	@GetMapping("/servicemapping/get")
	public ServiceMapping getServiceMapping(@RequestParam(value = "n") String namespace, @RequestParam(value = "t") String type) {
		return service.findBy(namespace, type);
	}

    /**
     * Find the service mappings of one namespace. For example asukaspysakointi and price, asukaspysakointi and availability, ...
     * 
     * @param namespace
     * @return ServiceMappings with backend service urls
     */
    @GetMapping("/servicemapping/getAll")
	public List<ServiceMapping> getAllServiceMapping(@RequestParam(value = "n") String namespace) {
		return service.findBy(namespace);
	}

    /**
     * Generate a service mapping for routing to namespace specific backend service.
     * For example for a product within tilavaraus namespace that is known within that namespace via it's local product id (namespaceEntityId). 
     * 
     * @param namespace
     * @param type
     * @param serviceUrl
     * @return ServiceMapping with backend service url
     */
    @GetMapping("/servicemapping/create")
	public ServiceMapping createServiceMapping(@RequestParam(value = "namespace") String namespace, @RequestParam(value = "type") String type, @RequestParam(value = "serviceUrl") String serviceUrl) {
		return service.createByParams(namespace, type, serviceUrl);
	}

    @GetMapping("/servicemapping/initializetestdata")
	public List<ServiceMapping> initializeTestData() {
        return service.initializeTestData();
	}
    
}
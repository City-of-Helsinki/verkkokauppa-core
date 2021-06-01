package fi.hel.verkkokauppa.productmapping.service;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import fi.hel.verkkokauppa.productmapping.model.ServiceConfiguration;
import fi.hel.verkkokauppa.productmapping.model.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.utils.UUIDGenerator;

@Component
public class ServiceConfigurationService {

    private Logger log = LoggerFactory.getLogger(ServiceConfigurationService.class);

    @Autowired
    private Environment env;
    
    @Autowired
    private ServiceConfigurationRepository serviceConfigurationRepository;


    public List<ServiceConfiguration> findBy(String namespace) {
        List<ServiceConfiguration> configurations = serviceConfigurationRepository.findByNamespace(namespace);
        return configurations;
    }

    public ServiceConfiguration findBy(String namespace, String configurationKey) {
        List<ServiceConfiguration> configurations = serviceConfigurationRepository.findByNamespaceAndConfigurationKey(namespace, configurationKey);
        return configurations.size() > 0 ? configurations.get(0) : null;
    }

    public ServiceConfiguration createByParams(String namespace, String configurationKey, String configurationValue) {
        String configurationId = UUIDGenerator.generateType3UUIDString(namespace, configurationKey);
        ServiceConfiguration serviceConfiguration = new ServiceConfiguration(configurationId, namespace, configurationKey, configurationValue);
        serviceConfigurationRepository.save(serviceConfiguration);
        log.debug("created service configuration for namespace: " + namespace + " with configurationId: " + configurationId);

        return serviceConfiguration;
    }

    // generate some mock data
    public List<ServiceConfiguration> initializeTestData() {
        String mockbackendurl = env.getProperty("mockbackend.url");

        List<ServiceConfiguration> entities = Arrays.asList(new ServiceConfiguration[]{
            createByParams("asukaspysakointi", ServiceConfigurationKeys.MERCHANT_ID, "asukaspysakointi_mock_merchant_id"),
            createByParams("asukaspysakointi", ServiceConfigurationKeys.TERMS_OF_USE_URL, mockbackendurl+"/mockserviceconfiguration/asukaspysakointi/terms_of_use"),

            createByParams("tilavaraus", ServiceConfigurationKeys.MERCHANT_ID, "tilavaraus_mock_merchant_id"),
            createByParams("tilavaraus", ServiceConfigurationKeys.TERMS_OF_USE_URL, mockbackendurl+"/mockserviceconfiguration/tilavaraus/terms_of_use"),
        });

        serviceConfigurationRepository.saveAll(entities);
        log.debug("initialized service configurations mock data");

        return entities;
    }

}

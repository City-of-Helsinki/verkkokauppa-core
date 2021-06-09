package fi.hel.verkkokauppa.productmapping.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import fi.hel.verkkokauppa.productmapping.model.ServiceConfiguration;
import fi.hel.verkkokauppa.productmapping.model.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;

@Component
public class ServiceConfigurationService {

    private Logger log = LoggerFactory.getLogger(ServiceConfigurationService.class);

    @Autowired
    private Environment env;
    
    @Autowired
    private ServiceConfigurationRepository serviceConfigurationRepository;


    public List<ServiceConfiguration> findBy(String namespace) {
        List<ServiceConfiguration> configurations = serviceConfigurationRepository.findByNamespaceAndRestrictedNot(namespace);
        configurations = configurations.stream().filter(conf -> conf.getRestricted() == false).collect(Collectors.toList());
        return configurations;
    }

    public ServiceConfiguration findBy(String namespace, String configurationKey) {
        List<ServiceConfiguration> configurations = serviceConfigurationRepository.findByNamespaceAndConfigurationKeyAndRestrictedNot(namespace, configurationKey);
        return configurations.size() > 0 ? configurations.get(0) : null;
    }

    public List<ServiceConfiguration> findRestricted(String namespace) {
        List<ServiceConfiguration> configurations = serviceConfigurationRepository.findByNamespace(namespace);
        configurations = configurations.stream().filter(conf -> conf.getRestricted() == false).collect(Collectors.toList());
        return configurations;
    }

    public ServiceConfiguration findRestricted(String namespace, String configurationKey) {
        List<ServiceConfiguration> configurations = serviceConfigurationRepository.findByNamespaceAndConfigurationKey(namespace, configurationKey);
        return configurations.size() > 0 ? configurations.get(0) : null;
    }

    public ServiceConfiguration createByParams(String namespace, String configurationKey, String configurationValue, boolean isRestricted) {
        String configurationId = UUIDGenerator.generateType3UUIDString(namespace, configurationKey);
        ServiceConfiguration serviceConfiguration = new ServiceConfiguration(configurationId, namespace, configurationKey, configurationValue, isRestricted);
        serviceConfigurationRepository.save(serviceConfiguration);
        log.debug("created service configuration for namespace: " + namespace + " with configurationId: " + configurationId);

        return serviceConfiguration;
    }

    // generate some mock data
    public List<ServiceConfiguration> initializeTestData() {
        String mockbackendurl = env.getProperty("mockbackend.url");

        List<ServiceConfiguration> entities = Arrays.asList(new ServiceConfiguration[]{
            createByParams("asukaspysakointi", ServiceConfigurationKeys.PAYMENT_MERCHANT_ID, "asukaspysakointi_mock_merchant_id", true),
            createByParams("asukaspysakointi", ServiceConfigurationKeys.TERMS_OF_USE_URL, mockbackendurl+"/mockserviceconfiguration/asukaspysakointi/terms_of_use", false),

            createByParams("tilavaraus", ServiceConfigurationKeys.PAYMENT_MERCHANT_ID, "tilavaraus_mock_merchant_id", true),
            createByParams("tilavaraus", ServiceConfigurationKeys.TERMS_OF_USE_URL, mockbackendurl+"/mockserviceconfiguration/tilavaraus/terms_of_use", false),
        });

        serviceConfigurationRepository.saveAll(entities);
        log.debug("initialized service configurations mock data");

        return entities;
    }

}

package fi.hel.verkkokauppa.productmapping.service.serviceConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import fi.hel.verkkokauppa.common.configuration.ServiceConfigurationKeys;
import fi.hel.verkkokauppa.productmapping.repository.serviceConfiguration.ServiceConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import fi.hel.verkkokauppa.productmapping.model.serviceConfiguration.ServiceConfiguration;

import fi.hel.verkkokauppa.common.util.UUIDGenerator;

@Component
public class ServiceConfigurationService {

    private Logger log = LoggerFactory.getLogger(ServiceConfigurationService.class);

    @Autowired
    private Environment env;
    
    @Autowired
    private ServiceConfigurationRepository serviceConfigurationRepository;

    public List<ServiceConfiguration> findBy(String namespace) {
        List<ServiceConfiguration> configurations = serviceConfigurationRepository.findByNamespaceAndRestricted(namespace, false);
        configurations = configurations.stream().filter(conf -> conf.getRestricted() == false).collect(Collectors.toList());
        return configurations;
    }

    public ServiceConfiguration findBy(String namespace, String configurationKey) {
        List<ServiceConfiguration> configurations = serviceConfigurationRepository.findByNamespaceAndConfigurationKeyAndRestricted(namespace, configurationKey, false);
        return configurations.size() > 0 ? configurations.get(0) : null;
    }

    public List<ServiceConfiguration> findRestricted(String namespace) {
        List<ServiceConfiguration> configurations = serviceConfigurationRepository.findByNamespace(namespace);
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

        List<ServiceConfiguration> entities = Arrays.asList(
                createByParams("asukaspysakointi", ServiceConfigurationKeys.PAYMENT_API_KEY, "asukaspysakointi_mock_api_key", true),
                createByParams("asukaspysakointi", ServiceConfigurationKeys.PAYMENT_ENCRYPTION_KEY, "asukaspysakointi_mock_encryption_key", true),
                createByParams("asukaspysakointi", ServiceConfigurationKeys.PAYMENT_RETURN_URL, mockbackendurl+"/mockserviceconfiguration/asukaspysakointi/return_url", true),
                createByParams("asukaspysakointi", ServiceConfigurationKeys.PAYMENT_NOTIFICATION_URL, mockbackendurl+"/mockserviceconfiguration/asukaspysakointi/notification_url", true),
                createByParams("asukaspysakointi", ServiceConfigurationKeys.PAYMENT_SUBMERCHANT_ID, "36240", true),
                createByParams("asukaspysakointi", ServiceConfigurationKeys.PAYMENT_CP, "PRO-31312-1", true),
                createByParams("asukaspysakointi", ServiceConfigurationKeys.MERCHANT_NAME, "asukaspysäköinti", false),
                createByParams("asukaspysakointi", ServiceConfigurationKeys.MERCHANT_STREET, "Katu 1", false),
                createByParams("asukaspysakointi", ServiceConfigurationKeys.MERCHANT_ZIP, "000000", false),
                createByParams("asukaspysakointi", ServiceConfigurationKeys.MERCHANT_CITY, "Helsinki", false),
                createByParams("asukaspysakointi", ServiceConfigurationKeys.MERCHANT_EMAIL, "asukas@pysäköinti.fi", false),
                createByParams("asukaspysakointi", ServiceConfigurationKeys.MERCHANT_PHONE, "123-456789", false),
                createByParams("asukaspysakointi", ServiceConfigurationKeys.MERCHANT_URL, mockbackendurl+"/mockserviceconfiguration/asukaspysakointi/url", false),
                createByParams("asukaspysakointi", ServiceConfigurationKeys.MERCHANT_TERMS_OF_SERVICE_URL, mockbackendurl+"/mockserviceconfiguration/asukaspysakointi/terms_of_use", false),
                createByParams("asukaspysakointi", ServiceConfigurationKeys.MERCHANT_PAYMENT_WEBHOOK_URL, mockbackendurl+"/mockserviceconfiguration/asukaspysakointi/merchant_payment_webhook", true),
                createByParams("asukaspysakointi", ServiceConfigurationKeys.ORDER_RIGHT_OF_PURCHASE_IS_ACTIVE, "true", true),
                createByParams("asukaspysakointi", ServiceConfigurationKeys.ORDER_RIGHT_OF_PURCHASE_URL, mockbackendurl+"/backend/asukaspysakointi/order/right-of-purchase", true),

                createByParams("venepaikat", ServiceConfigurationKeys.PAYMENT_API_KEY, "venepaikat_mock_api_key", true),
                createByParams("venepaikat", ServiceConfigurationKeys.PAYMENT_ENCRYPTION_KEY, "venepaikat_mock_encryption_key", true),
                createByParams("venepaikat", ServiceConfigurationKeys.PAYMENT_RETURN_URL, mockbackendurl+"/mockserviceconfiguration/venepaikat/return_url", true),
                createByParams("venepaikat", ServiceConfigurationKeys.PAYMENT_NOTIFICATION_URL, mockbackendurl+"/mockserviceconfiguration/venepaikat/notification_url", true),
                createByParams("venepaikat", ServiceConfigurationKeys.PAYMENT_SUBMERCHANT_ID, "36240", true),
                createByParams("venepaikat", ServiceConfigurationKeys.PAYMENT_CP, "PRO-31312-1", true),
                createByParams("venepaikat", ServiceConfigurationKeys.MERCHANT_NAME, "venepaikat", false),
                createByParams("venepaikat", ServiceConfigurationKeys.MERCHANT_STREET, "Ranta 1", false),
                createByParams("venepaikat", ServiceConfigurationKeys.MERCHANT_ZIP, "000000", false),
                createByParams("venepaikat", ServiceConfigurationKeys.MERCHANT_CITY, "Helsinki", false),
                createByParams("venepaikat", ServiceConfigurationKeys.MERCHANT_EMAIL, "vene@paikat.fi", false),
                createByParams("venepaikat", ServiceConfigurationKeys.MERCHANT_PHONE, "123-456789", false),
                createByParams("venepaikat", ServiceConfigurationKeys.MERCHANT_URL, mockbackendurl+"/mockserviceconfiguration/venepaikat/url", false),
                createByParams("venepaikat", ServiceConfigurationKeys.MERCHANT_TERMS_OF_SERVICE_URL, mockbackendurl+"/mockserviceconfiguration/venepaikat/terms_of_use", false),
                createByParams("venepaikat", ServiceConfigurationKeys.MERCHANT_PAYMENT_WEBHOOK_URL, mockbackendurl+"/mockserviceconfiguration/venepaikat/merchant_payment_webhook", true),
                createByParams("venepaikat", ServiceConfigurationKeys.ORDER_RIGHT_OF_PURCHASE_IS_ACTIVE, "true", true),
                createByParams("venepaikat", ServiceConfigurationKeys.ORDER_RIGHT_OF_PURCHASE_URL, mockbackendurl+"/backend/venepaikat/order/right-of-purchase", true)

                );

        serviceConfigurationRepository.saveAll(entities);
        log.debug("initialized service configurations mock data");

        return entities;
    }

}

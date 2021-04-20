package fi.hel.verkkokauppa.productmapping.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import fi.hel.verkkokauppa.productmapping.model.ServiceMapping;
import fi.hel.verkkokauppa.utils.UUIDGenerator;

@Component
public class ServiceMappingService {
    
    private Logger log = LoggerFactory.getLogger(ServiceMappingService.class);

    @Autowired
    private Environment env;
        
    @Autowired
    private ServiceMappingRepository serviceMappingRepository;


    public List<ServiceMapping> findBy(String namespace) {
        Iterable<ServiceMapping> mappings = serviceMappingRepository.findAll();
        List<ServiceMapping> result = new ArrayList<ServiceMapping>();
        mappings.forEach(result::add);

        List<ServiceMapping> filteredMappings = result.stream()
            .filter(mapping -> mapping.getNamespace().equals(namespace)).collect(Collectors.toList());

        return filteredMappings;
    }

    public ServiceMapping findBy(String namespace, String type) {
        Iterable<ServiceMapping> mappings = serviceMappingRepository.findAll();
        List<ServiceMapping> result = new ArrayList<ServiceMapping>();
        mappings.forEach(result::add);

        List<ServiceMapping> filteredMappings = result.stream()
            .filter(mapping -> mapping.getNamespace().equals(namespace) && mapping.getType().equals(type)).collect(Collectors.toList());

        return filteredMappings.size() > 0 ? filteredMappings.get(0) : null;
    }

    public ServiceMapping createByParams(String namespace, String type, String serviceUrl) {
        String serviceId = UUIDGenerator.generateType3UUIDString(namespace, type);
        ServiceMapping serviceMapping = new ServiceMapping(serviceId, namespace, type, serviceUrl);
        serviceMappingRepository.save(serviceMapping);

        return serviceMapping;
    }

    // generate some mock data
    public List<ServiceMapping> initializeTestData() {
        String mockbackendurl = env.getProperty("mockbackend.url");

        List<ServiceMapping> entities = Arrays.asList(new ServiceMapping[]{
            createByParams("asukaspysakointi", "product", mockbackendurl+"/mockproductmanagement/asukaspysakointi/get?productId="),
            createByParams("asukaspysakointi", "price", mockbackendurl+"/mockprice/asukaspysakointi/get?productId="),
            createByParams("asukaspysakointi", "rightofpurchase", "http://asukaspysakointibackendservice/api/rightofpurchase/get?productId="),
            createByParams("asukaspysakointi", "availability", "http://asukaspysakointibackendservice/api/availability/get?productId="),

            createByParams("tilavaraus", "product", mockbackendurl+"/mockproductmanagement/tilavaraus/get?productId="),
            createByParams("tilavaraus", "price", mockbackendurl+"/mockprice/tilavaraus/get?productId="),
            createByParams("tilavaraus", "rightofpurchase", "http://tilavarausbackendservice/api/rightofpurchase/get?productId="),
            createByParams("tilavaraus", "availability", "http://tilavarausbackendservice/api/availability/get?productId="),
        });

        serviceMappingRepository.saveAll(entities);

        return entities;
    }

}

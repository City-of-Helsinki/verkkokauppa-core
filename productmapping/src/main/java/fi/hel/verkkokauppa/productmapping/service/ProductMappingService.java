package fi.hel.verkkokauppa.productmapping.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.hel.verkkokauppa.productmapping.model.ProductMapping;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;

@Component
public class ProductMappingService {

    private Logger log = LoggerFactory.getLogger(ProductMappingService.class);
    
    @Autowired
    private ProductMappingRepository productMappingRepository;


    public ProductMapping findById(String productId) {
        Optional<ProductMapping> mapping = productMappingRepository.findById(productId);
        
        if (mapping.isPresent())
            return mapping.get();

        log.debug("product mapping not found, productId: " + productId);
        return null;
    }

    public ProductMapping createByParams(String namespace, String namespaceEntityId) {
        String productId = UUIDGenerator.generateType3UUIDString(namespace, namespaceEntityId);
        ProductMapping productMapping = new ProductMapping(productId, namespace, namespaceEntityId);
        productMappingRepository.save(productMapping);

        return productMapping;
    }

    // generate some mock data
    public List<ProductMapping> initializeTestData() {
        List<ProductMapping> entities = Arrays.asList(new ProductMapping[]{
            createByParams("asukaspysakointi", "1234"),
            createByParams("asukaspysakointi", "12345"),
            createByParams("asukaspysakointi", "123456"),
            createByParams("tilavaraus", "1234"),
            createByParams("tilavaraus", "12345"),
            createByParams("tilavaraus", "123456")
        });

        productMappingRepository.saveAll(entities);

        return entities;
    }

}

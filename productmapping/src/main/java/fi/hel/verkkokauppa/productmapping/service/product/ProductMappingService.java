package fi.hel.verkkokauppa.productmapping.service.product;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import fi.hel.verkkokauppa.productmapping.repository.product.ProductMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.hel.verkkokauppa.productmapping.model.product.ProductMapping;
import fi.hel.verkkokauppa.common.util.UUIDGenerator;

@Component
public class ProductMappingService {

    private Logger log = LoggerFactory.getLogger(ProductMappingService.class);
    
    @Autowired
    private ProductMappingRepository productMappingRepository;


    public List<ProductMapping> findBy(String namespace) {
        List<ProductMapping> mappings = productMappingRepository.findByNamespace(namespace);
        return mappings;
    }

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
        log.debug("created product mapping for namespace: " + namespace + " with productId: " + productId);

        return productMapping;
    }

    // generate some mock data
    public List<ProductMapping> initializeTestData() {
        List<ProductMapping> entities = Arrays.asList(
                createByParams("asukaspysakointi", "1234"),
                createByParams("asukaspysakointi", "12345"),
                createByParams("asukaspysakointi", "123456"),
                createByParams("tilavaraus", "1234"),
                createByParams("tilavaraus", "12345"),
                createByParams("tilavaraus", "123456"),
                createByParams("venepaikat", "1234"),
                createByParams("venepaikat", "12345"),
                createByParams("venepaikat", "5678")
        );

        productMappingRepository.saveAll(entities);
        log.debug("initialized product mappings mock data");

        return entities;
    }

}

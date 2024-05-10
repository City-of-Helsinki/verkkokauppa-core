package fi.hel.verkkokauppa.productmapping.service.product;

import fi.hel.verkkokauppa.common.util.UUIDGenerator;
import fi.hel.verkkokauppa.productmapping.model.product.ProductMapping;
import fi.hel.verkkokauppa.productmapping.repository.product.ProductMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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

    public ProductMapping createByParams(String namespace, String namespaceEntityId, String merchantId) {
        String productId = generateProductIdFromMerchantId(namespace, namespaceEntityId, merchantId);
        ProductMapping productMapping = new ProductMapping(productId, namespace, namespaceEntityId, merchantId);
        productMappingRepository.save(productMapping);
        log.debug("created product mapping for namespace: " + namespace + " and merchant: " + merchantId + " with productId: " + productId);

        return productMapping;
    }

    public String generateProductIdFromMerchantId(String namespace, String namespaceEntityId, String merchantId) {
        String productId = UUIDGenerator.generateType3UUIDString(namespace, namespaceEntityId);
        productId = UUIDGenerator.generateType3UUIDString(productId , merchantId);
        return productId;
    }


    // generate some mock data
    public List<ProductMapping> initializeTestData() {
        List<ProductMapping> entities = Arrays.asList(
                createByParams("asukaspysakointi", "1234", "9876"),
                createByParams("asukaspysakointi", "12345", "9876"),
                createByParams("asukaspysakointi", "123456","9876"),
                createByParams("tilavaraus", "1234", "98765"),
                createByParams("tilavaraus", "12345", "98765"),
                createByParams("tilavaraus", "123456", "98765"),
                createByParams("venepaikat", "1234", "987654"),
                createByParams("venepaikat", "12345", "987654"),
                createByParams("venepaikat", "5678", "987654")
        );

        productMappingRepository.saveAll(entities);
        log.debug("initialized product mappings mock data");

        return entities;
    }

    public ProductMapping findByNamespaceEntityId(String namespaceEntityId) {
        return this.productMappingRepository.findByNamespaceEntityId(namespaceEntityId);
    }
}

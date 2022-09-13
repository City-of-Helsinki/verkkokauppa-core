package fi.hel.verkkokauppa.productmapping.service;

import fi.hel.verkkokauppa.productmapping.model.product.ProductMapping;
import fi.hel.verkkokauppa.productmapping.repository.product.ProductMappingRepository;
import fi.hel.verkkokauppa.productmapping.service.product.ProductMappingService;
import fi.hel.verkkokauppa.productmapping.testing.annotations.RunIfProfile;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@RunIfProfile(profile = "local")
public class ProductMappingServiceTest {

    private static final Logger LOG = LoggerFactory.getLogger(ProductMappingServiceTest.class);

    @Autowired
    private ProductMappingService productMappingService;

    @Autowired
    private ProductMappingRepository productMappingRepository;

    private List<ProductMapping> toBeDeleted = new ArrayList();

    @AfterEach
    public void tearDown() {
        try {
            productMappingRepository.deleteAll(toBeDeleted);
            // Clear list because all merchants deleted
            toBeDeleted = new ArrayList<>();
        } catch (Exception e) {
            LOG.info("delete error {}", e.toString());
        }

    }

    @Test
    public void testCreateProductMappingByParams() {
        String namespace = "testNamespace";
        String namespaceEntityId = "1234";
        String merchantId = "9876";
        String expectedProductId = productMappingService.generateProductIdFromMerchantId(namespace, namespaceEntityId, merchantId);

        ProductMapping mapping = productMappingService.createByParams(namespace, namespaceEntityId, merchantId);
        Assertions.assertEquals(mapping.getProductId(), expectedProductId);
        toBeDeleted.add(mapping);
    }




}

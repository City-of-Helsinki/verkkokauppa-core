package fi.hel.verkkokauppa.product.service;

import fi.hel.verkkokauppa.product.ProductAccountingTransformer;
import fi.hel.verkkokauppa.product.dto.ProductAccountingDto;
import fi.hel.verkkokauppa.product.model.ProductAccounting;
import fi.hel.verkkokauppa.product.repository.ProductAccountingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProductAccountingService {

    private Logger log = LoggerFactory.getLogger(ProductAccountingService.class);

    @Autowired
    private ProductAccountingRepository productAccountingRepository;

    public ProductAccounting createProductAccounting(ProductAccountingDto productAccountingDto) {
        ProductAccounting productAccountingEntity = new ProductAccountingTransformer().transform(productAccountingDto);
        this.productAccountingRepository.save(productAccountingEntity);
        return productAccountingEntity;
    }

    public ProductAccounting getProductAccounting(String productId) {
        Optional<ProductAccounting> mapping = productAccountingRepository.findById(productId);

        if (mapping.isPresent())
            return mapping.get();

        log.warn("product accounting not found, productId: " + productId);
        return null;
    }
}

package fi.hel.verkkokauppa.product.service;

import fi.hel.verkkokauppa.product.ProductAccountingTransformer;
import fi.hel.verkkokauppa.product.dto.ProductAccountingDto;
import fi.hel.verkkokauppa.product.model.Product;
import fi.hel.verkkokauppa.product.model.ProductAccounting;
import fi.hel.verkkokauppa.product.repository.ProductAccountingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductAccountingService {

    @Autowired
    private ProductAccountingRepository productAccountingRepository;

    public ProductAccounting createProductAccounting(ProductAccountingDto productAccountingDto) {
        ProductAccounting productAccountingEntity = new ProductAccountingTransformer().transform(productAccountingDto);
        this.productAccountingRepository.save(productAccountingEntity);
        return productAccountingEntity;
    }
}

package fi.hel.verkkokauppa.product.api;

import fi.hel.verkkokauppa.product.dto.ProductInvoicingDto;
import fi.hel.verkkokauppa.product.mapper.ProductInvoicingMapper;
import fi.hel.verkkokauppa.product.model.ProductInvoicing;
import fi.hel.verkkokauppa.product.repository.ProductInvoicingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductInvoicingController {
    private Logger log = LoggerFactory.getLogger(ProductInvoicingController.class);

    @Autowired
    ProductInvoicingMapper productInvoicingMapper;

    @Autowired
    ProductInvoicingRepository productInvoicingRepository;

    @PostMapping("/product/invoicing")
    public ResponseEntity<ProductInvoicingDto> createProductInvoicing(@RequestBody ProductInvoicingDto productInvoicingDto) {
        ProductInvoicing productInvoicing = productInvoicingRepository.save(productInvoicingMapper.fromDto(productInvoicingDto));
        return ResponseEntity.ok().body(productInvoicingMapper.toDto(productInvoicing));
    }
}

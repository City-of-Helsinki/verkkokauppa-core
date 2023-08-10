package fi.hel.verkkokauppa.product.repository;

import fi.hel.verkkokauppa.product.model.ProductInvoicing;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductInvoicingRepository extends ElasticsearchRepository<ProductInvoicing, String> {
}

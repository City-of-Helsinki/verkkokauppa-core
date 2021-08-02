package fi.hel.verkkokauppa.product.repository;

import fi.hel.verkkokauppa.product.model.Product;
import fi.hel.verkkokauppa.product.model.ProductAccounting;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductAccountingRepository extends ElasticsearchRepository<ProductAccounting, String> {
}

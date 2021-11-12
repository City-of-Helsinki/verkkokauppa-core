package fi.hel.verkkokauppa.productmapping.repository.product;

import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import fi.hel.verkkokauppa.productmapping.model.product.ProductMapping;

@Repository
public interface ProductMappingRepository extends ElasticsearchRepository<ProductMapping, String> {

    List<ProductMapping> findByNamespace(String namespace);

}

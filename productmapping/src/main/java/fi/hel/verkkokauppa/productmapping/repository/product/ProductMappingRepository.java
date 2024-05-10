package fi.hel.verkkokauppa.productmapping.repository.product;

import fi.hel.verkkokauppa.productmapping.model.product.ProductMapping;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductMappingRepository extends ElasticsearchRepository<ProductMapping, String> {

    List<ProductMapping> findByNamespace(String namespace);

    ProductMapping findByNamespaceEntityId(String namespaceEntityId);
}

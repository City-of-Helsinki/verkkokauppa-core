package fi.hel.verkkokauppa.product.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import fi.hel.verkkokauppa.product.model.Product;

@Repository
public interface ProductRepository extends ElasticsearchRepository<Product, String> {
}
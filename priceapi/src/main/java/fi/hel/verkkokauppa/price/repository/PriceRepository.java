package fi.hel.verkkokauppa.price.repository;

import fi.hel.verkkokauppa.price.model.PriceModel;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PriceRepository extends ElasticsearchRepository<PriceModel, String> {
    PriceModel findByProductId(String productId);
}
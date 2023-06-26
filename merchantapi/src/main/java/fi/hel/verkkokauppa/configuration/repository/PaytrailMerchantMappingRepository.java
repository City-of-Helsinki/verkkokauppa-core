package fi.hel.verkkokauppa.configuration.repository;

import fi.hel.verkkokauppa.configuration.model.merchant.PaytrailMerchantMapping;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaytrailMerchantMappingRepository extends ElasticsearchRepository<PaytrailMerchantMapping, String> {
}

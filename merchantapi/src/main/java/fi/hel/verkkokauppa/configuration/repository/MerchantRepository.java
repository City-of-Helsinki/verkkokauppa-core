package fi.hel.verkkokauppa.configuration.repository;


import fi.hel.verkkokauppa.configuration.model.merchant.MerchantModel;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MerchantRepository extends ElasticsearchRepository<MerchantModel, String> {
    MerchantModel findByMerchantIdAndNamespace(String merchantId, String namespace);

    List<MerchantModel> findAllByNamespace(String namespace);
}

package fi.hel.verkkokauppa.merchant.repository;


import fi.hel.verkkokauppa.merchant.model.MerchantModel;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MerchantRepository extends ElasticsearchRepository<MerchantModel, String> {
    MerchantModel findByMerchantIdAndNamespace(String merchantId, String namespace);

    List<MerchantModel> findAllByNamespace(String namespace);
}

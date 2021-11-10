package fi.hel.verkkokauppa.order.repository.jpa;

import fi.hel.verkkokauppa.order.model.renewal.SubscriptionRenewalRequest;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRenewalRequestRepository extends ElasticsearchRepository<SubscriptionRenewalRequest, String> {

}

package fi.hel.verkkokauppa.order.repository.jpa;

import fi.hel.verkkokauppa.order.model.subscription.email.SubscriptionCardExpired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionCardExpiredRepository extends ElasticsearchRepository<SubscriptionCardExpired, String> {

}
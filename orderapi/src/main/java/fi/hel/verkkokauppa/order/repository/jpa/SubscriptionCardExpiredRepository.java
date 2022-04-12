package fi.hel.verkkokauppa.order.repository.jpa;

import fi.hel.verkkokauppa.order.model.subscription.email.SubscriptionCardExpired;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionCardExpiredRepository extends ElasticsearchRepository<SubscriptionCardExpired, String> {
    List<SubscriptionCardExpired> findAllBySubscriptionId(String subscriptionId);
}
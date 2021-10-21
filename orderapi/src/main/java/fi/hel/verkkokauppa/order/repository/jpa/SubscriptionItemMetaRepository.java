package fi.hel.verkkokauppa.order.repository.jpa;

import fi.hel.verkkokauppa.order.model.OrderItemMeta;
import fi.hel.verkkokauppa.order.model.subscription.SubscriptionItemMeta;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionItemMetaRepository extends ElasticsearchRepository<SubscriptionItemMeta, String> {

    List<SubscriptionItemMeta> findByOrderId(String orderId);
    List<SubscriptionItemMeta> findByOrderItemId(String orderItemId);
    List<SubscriptionItemMeta> findByOrderItemIdAndKey(String orderItemId, String key);

}
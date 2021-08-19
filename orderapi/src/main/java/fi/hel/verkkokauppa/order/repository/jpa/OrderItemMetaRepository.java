package fi.hel.verkkokauppa.order.repository.jpa;

import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import fi.hel.verkkokauppa.order.model.OrderItemMeta;

@Repository
public interface OrderItemMetaRepository extends ElasticsearchRepository<OrderItemMeta, String> {

    List<OrderItemMeta> findByOrderId(String orderId);
    List<OrderItemMeta> findByOrderItemId(String orderItemId);
    List<OrderItemMeta> findByOrderItemIdAndKey(String orderItemId, String key);

}
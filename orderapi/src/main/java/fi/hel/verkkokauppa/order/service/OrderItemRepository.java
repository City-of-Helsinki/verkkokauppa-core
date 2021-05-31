package fi.hel.verkkokauppa.order.service;

import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import fi.hel.verkkokauppa.order.model.OrderItem;

@Repository
public interface OrderItemRepository extends ElasticsearchRepository<OrderItem, String> {

    List<OrderItem> findByOrderId(String orderId);
}

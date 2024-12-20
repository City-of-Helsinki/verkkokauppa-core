package fi.hel.verkkokauppa.order.repository.jpa;

import fi.hel.verkkokauppa.order.model.Order;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends ElasticsearchRepository<Order, String> {

    List<Order> findByNamespaceAndUser(String namespace, String user);
    List<Order> findByUser(String user);
    List<Order> findOrdersBySubscriptionId(String subscriptionId);
    List<Order> findOrdersBySubscriptionIdAndEndDate(String subscriptionId, LocalDateTime endDate);
    List<Order> findOrdersBySubscriptionIdAndEndDateAndStatus(String subscriptionId, LocalDateTime endDate, String status);

}
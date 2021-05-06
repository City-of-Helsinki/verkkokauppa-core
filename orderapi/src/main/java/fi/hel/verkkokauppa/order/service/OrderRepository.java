package fi.hel.verkkokauppa.order.service;

import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import fi.hel.verkkokauppa.order.model.Order;

@Repository
public interface OrderRepository extends ElasticsearchRepository<Order, String> {

    List<Order> findByNamespaceAndUser(String namespace, String user);

}
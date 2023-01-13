package fi.hel.verkkokauppa.order.repository.jpa;

import fi.hel.verkkokauppa.order.model.OrderPaymentMethod;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderPaymentMethodRepository extends ElasticsearchRepository<OrderPaymentMethod, String> {
    List<OrderPaymentMethod> findByOrderId(String orderId);
    long deleteByOrderId(String orderId);
}

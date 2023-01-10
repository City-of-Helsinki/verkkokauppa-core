package fi.hel.verkkokauppa.payment.repository;

import fi.hel.verkkokauppa.payment.model.OrderPaymentMethod;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface OrderPaymentMethodRepository extends ElasticsearchRepository<OrderPaymentMethod, String> {
    List<OrderPaymentMethod> findByOrderId(String orderId);
    long deleteByOrderId(String orderId);
}

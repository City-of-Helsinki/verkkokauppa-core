package fi.hel.verkkokauppa.order.repository.jpa;

import fi.hel.verkkokauppa.order.model.accounting.OrderItemAccounting;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemAccountingRepository extends ElasticsearchRepository<OrderItemAccounting, String> {

    List<OrderItemAccounting> findByOrderId(String orderId);

}

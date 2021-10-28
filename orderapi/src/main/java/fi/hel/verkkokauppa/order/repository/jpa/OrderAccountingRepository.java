package fi.hel.verkkokauppa.order.repository.jpa;

import fi.hel.verkkokauppa.order.model.accounting.OrderAccounting;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderAccountingRepository extends ElasticsearchRepository<OrderAccounting, String> {

    OrderAccounting findByOrderId(String orderId);

}

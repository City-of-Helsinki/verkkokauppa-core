package fi.hel.verkkokauppa.order.repository.jpa;

import fi.hel.verkkokauppa.order.model.invoice.OrderItemInvoicing;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemInvoicingRepository extends ElasticsearchRepository<OrderItemInvoicing, String> {
}

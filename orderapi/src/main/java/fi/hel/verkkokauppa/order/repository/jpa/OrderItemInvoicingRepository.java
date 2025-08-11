package fi.hel.verkkokauppa.order.repository.jpa;

import fi.hel.verkkokauppa.order.model.invoice.OrderItemInvoicing;
import fi.hel.verkkokauppa.order.model.invoice.OrderItemInvoicingStatus;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderItemInvoicingRepository extends ElasticsearchRepository<OrderItemInvoicing, String> {
    List<OrderItemInvoicing> findAllByInvoicingDateLessThanEqualAndStatus(LocalDate invoicingDate, OrderItemInvoicingStatus status);
    List<OrderItemInvoicing> findByOrderId(String orderId);
}

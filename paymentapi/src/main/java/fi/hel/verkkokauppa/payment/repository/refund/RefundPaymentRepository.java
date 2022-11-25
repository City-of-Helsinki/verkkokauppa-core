package fi.hel.verkkokauppa.payment.repository.refund;

import fi.hel.verkkokauppa.payment.model.refund.RefundPayment;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundPaymentRepository extends ElasticsearchRepository<RefundPayment, String> {

    List<RefundPayment> findByOrderId(String orderId);
    List<RefundPayment> findByNamespaceAndOrderId(String namespace, String orderId);
    List<RefundPayment> findByNamespaceAndOrderIdAndStatus(String namespace, String orderId, String status);

}
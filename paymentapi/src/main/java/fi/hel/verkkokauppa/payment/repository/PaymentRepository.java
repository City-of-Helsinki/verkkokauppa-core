package fi.hel.verkkokauppa.payment.repository;

import fi.hel.verkkokauppa.payment.model.Payment;

import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends ElasticsearchRepository<Payment, String> {

    List<Payment> findByOrderId(String orderId);
    List<Payment> findByNamespaceAndOrderId(String namespace, String orderId);
    List<Payment> findByNamespaceAndOrderIdAndStatus(String namespace, String orderId, String status);

}
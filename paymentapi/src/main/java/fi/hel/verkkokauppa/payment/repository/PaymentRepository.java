package fi.hel.verkkokauppa.payment.repository;

import fi.hel.verkkokauppa.payment.constant.PaymentGatewayEnum;
import fi.hel.verkkokauppa.payment.model.Payment;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends ElasticsearchRepository<Payment, String> {

    List<Payment> findByOrderId(String orderId);
    List<Payment> findByNamespaceAndOrderId(String namespace, String orderId);
    List<Payment> findByNamespaceAndOrderIdAndStatus(String namespace, String orderId, String status);

    List<Payment> findByPaymentGatewayAndStatusAndCreatedAtBetweenAndPaytrailTransactionIdIsNotNull(PaymentGatewayEnum gateway, String status, LocalDateTime createdBefore, LocalDateTime createdAfter);

    Payment findByPaytrailTransactionId(String paytrailTransactionId);
    Payment findByPaymentId(String paymentId);
}
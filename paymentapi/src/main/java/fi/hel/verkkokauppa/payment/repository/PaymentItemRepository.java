package fi.hel.verkkokauppa.payment.repository;

import fi.hel.verkkokauppa.payment.model.PaymentItem;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentItemRepository extends ElasticsearchRepository<PaymentItem, String> {

    long deleteByPaymentId(String paymentId);
    List<PaymentItem> findByPaymentId(String paymentId);
}
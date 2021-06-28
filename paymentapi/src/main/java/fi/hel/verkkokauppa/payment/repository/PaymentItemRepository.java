package fi.hel.verkkokauppa.payment.repository;

import fi.hel.verkkokauppa.payment.model.PaymentItem;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentItemRepository extends ElasticsearchRepository<PaymentItem, String> {
}
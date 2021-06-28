package fi.hel.verkkokauppa.payment.repository;

import fi.hel.verkkokauppa.payment.model.Payment;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends ElasticsearchRepository<Payment, String> {
}
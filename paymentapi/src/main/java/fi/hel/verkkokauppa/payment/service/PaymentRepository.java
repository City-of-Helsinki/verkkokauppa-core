package fi.hel.verkkokauppa.payment.service;

import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import fi.hel.verkkokauppa.payment.model.Payment;


@Repository
public interface PaymentRepository extends ElasticsearchRepository<Payment, String> {

    List<Payment> findByNamespace(String namespace);

}

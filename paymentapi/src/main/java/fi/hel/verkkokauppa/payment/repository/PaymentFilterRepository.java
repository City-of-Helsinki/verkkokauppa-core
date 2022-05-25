package fi.hel.verkkokauppa.payment.repository;

import fi.hel.verkkokauppa.payment.model.PaymentFilter;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface PaymentFilterRepository extends ElasticsearchRepository<PaymentFilter, String> {
    List<PaymentFilter> findAllByReferenceId(String referenceId);
}

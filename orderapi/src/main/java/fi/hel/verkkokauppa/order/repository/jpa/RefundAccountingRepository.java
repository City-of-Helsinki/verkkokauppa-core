package fi.hel.verkkokauppa.order.repository.jpa;

import fi.hel.verkkokauppa.order.model.accounting.RefundAccounting;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefundAccountingRepository extends ElasticsearchRepository<RefundAccounting, String> {

    RefundAccounting findByRefundId(String refundId);

}

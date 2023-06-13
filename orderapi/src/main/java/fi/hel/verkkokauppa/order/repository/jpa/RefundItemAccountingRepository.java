package fi.hel.verkkokauppa.order.repository.jpa;

import fi.hel.verkkokauppa.order.model.accounting.RefundItemAccounting;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundItemAccountingRepository extends ElasticsearchRepository<RefundItemAccounting, String> {

    List<RefundItemAccounting> findByRefundId(String refundId);

}

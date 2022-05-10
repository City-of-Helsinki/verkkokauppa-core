package fi.hel.verkkokauppa.order.repository.jpa;

import fi.hel.verkkokauppa.order.model.refund.RefundItem;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface RefundItemRepository extends ElasticsearchRepository<RefundItem, String> {
  List<RefundItem> findByRefundId(String refundId);
}

package fi.hel.verkkokauppa.order.repository.jpa;

import fi.hel.verkkokauppa.order.model.refund.RefundItem;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface RefundItemRepository extends ElasticsearchRepository<RefundItem, String> {
}

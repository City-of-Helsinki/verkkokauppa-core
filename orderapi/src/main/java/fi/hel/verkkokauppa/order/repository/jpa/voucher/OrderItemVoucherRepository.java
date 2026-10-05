package fi.hel.verkkokauppa.order.repository.jpa.voucher;

import fi.hel.verkkokauppa.order.model.voucher.OrderItemVoucher;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface OrderItemVoucherRepository extends ElasticsearchRepository<OrderItemVoucher, String> {

}

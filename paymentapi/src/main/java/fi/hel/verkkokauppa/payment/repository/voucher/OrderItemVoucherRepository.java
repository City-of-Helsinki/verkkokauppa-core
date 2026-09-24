package fi.hel.verkkokauppa.payment.repository.voucher;

import fi.hel.verkkokauppa.payment.model.voucher.OrderItemVoucher;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface OrderItemVoucherRepository extends ElasticsearchRepository<OrderItemVoucher, String> {

}

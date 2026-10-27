package fi.hel.verkkokauppa.order.repository.jpa.voucher;

import fi.hel.verkkokauppa.order.model.voucher.ReservedVoucherCode;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ReservedVoucherCodeRepository extends ElasticsearchRepository<ReservedVoucherCode, String> {
    boolean existsByCode(String code);

}

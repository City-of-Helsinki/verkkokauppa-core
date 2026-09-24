package fi.hel.verkkokauppa.payment.repository.voucher;

import fi.hel.verkkokauppa.payment.model.ReservedVoucherCode;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ReservedVoucherCodeRepository extends ElasticsearchRepository<ReservedVoucherCode, String> {
    boolean existsByCode(String code);

}

package fi.hel.verkkokauppa.payment.repository;

import fi.hel.verkkokauppa.payment.model.ReservedVoucherCode;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ReservedVoucherCodeRepository extends ElasticsearchRepository<ReservedVoucherCode, String> {
    boolean existsByCode(String code);

}

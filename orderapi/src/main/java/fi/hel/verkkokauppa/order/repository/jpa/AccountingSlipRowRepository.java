package fi.hel.verkkokauppa.order.repository.jpa;

import fi.hel.verkkokauppa.order.model.accounting.AccountingSlipRow;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountingSlipRowRepository extends ElasticsearchRepository<AccountingSlipRow, String> {

    List<AccountingSlipRow> findByAccountingSlipId(String accountingSlipId);

}

package fi.hel.verkkokauppa.order.repository.jpa;

import fi.hel.verkkokauppa.order.model.accounting.AccountingSlip;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountingSlipRepository extends ElasticsearchRepository<AccountingSlip, String> {

    int countAccountingSlipsByReferenceContains(String year);

}

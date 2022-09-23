package fi.hel.verkkokauppa.order.repository.jpa;

import fi.hel.verkkokauppa.order.model.accounting.AccountingExportData;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AccountingExportDataRepository extends ElasticsearchRepository<AccountingExportData, String> {

    List<AccountingExportData> findAllByTimestamp(LocalDate timestamp);

    int countAllByExportedGreaterThanEqualAndExportedLessThanEqual(LocalDate start, LocalDate end);

}

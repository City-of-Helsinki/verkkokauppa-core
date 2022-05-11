package fi.hel.verkkokauppa.order.repository.jpa;

import fi.hel.verkkokauppa.order.model.accounting.AccountingExportData;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountingExportDataRepository extends ElasticsearchRepository<AccountingExportData, String> {

    List<AccountingExportData> findAllByTimestamp(String timestamp);

    int countAllByExportedStartsWith(String year);

}

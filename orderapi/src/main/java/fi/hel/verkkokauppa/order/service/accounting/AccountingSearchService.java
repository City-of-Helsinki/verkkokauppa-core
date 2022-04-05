package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.accounting.AccountingExportData;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountingSearchService {

    @Autowired
    private ElasticsearchOperations operations;

    public List<AccountingExportData> getNotExportedAccountingExportData() {
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("exported"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb)
                .withPageable(PageRequest.of(0, 10000))
                .build();

        SearchHits<AccountingExportData> hits = operations.search(query, AccountingExportData.class);

        SearchPage<AccountingExportData> searchHits = SearchHitSupport.searchPageFor(hits, query.getPageable());

        final List<AccountingExportData> exportData = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        if (exportData.isEmpty()) {
            return new ArrayList<>();
        }

        return exportData;
    }

    public List<Order> findNotAccounted() {
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("accounted"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb)
                .withPageable(PageRequest.of(0, 10000))
                .build();
        SearchHits<Order> hits = operations.search(query, Order.class);

        SearchPage<Order> searchHits = SearchHitSupport.searchPageFor(hits, query.getPageable());

        final List<Order> exportData = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        if (exportData.isEmpty()) {
            return new ArrayList<>();
        }

        return exportData;
    }

}

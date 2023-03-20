package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.accounting.AccountingExportData;
import fi.hel.verkkokauppa.order.service.elasticsearch.SearchAfterService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AccountingSearchService {

    @Autowired
    private SearchAfterService searchAfterService;

    public List<AccountingExportData> getNotExportedAccountingExportData() throws Exception {
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("exported"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb).build();

        SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                query,
                searchAfterService.buildSortWithId(),
                AccountingExportData.INDEX_NAME
        );

        log.info(searchRequest.toString());
        SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

        final List<AccountingExportData> exportData = searchAfterService.buildListFromHits(hits, AccountingExportData.class);

        return exportData;
    }

    public List<Order> findNotAccounted() throws Exception {
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("accounted"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb).build();

        SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                query,
                searchAfterService.buildSortWithId(),
                Order.INDEX_NAME
        );

        log.info(searchRequest.toString());
        SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

        final List<Order> exportData = searchAfterService.buildListFromHits(hits, Order.class);

        return exportData;
    }

}

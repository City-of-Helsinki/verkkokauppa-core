package fi.hel.verkkokauppa.order.service.accounting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.accounting.AccountingExportData;
import fi.hel.verkkokauppa.order.service.elasticsearch.SearchAfterService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AccountingSearchService {

    @Autowired
    private SearchAfterService searchAfterService;

    @Autowired
    private ObjectMapper objectMapper;

    public List<AccountingExportData> getNotExportedAccountingExportData() throws Exception {
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("exported"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb).build();

        SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                query,
                new SortBuilder[] {new FieldSortBuilder("_id").order(SortOrder.DESC)},
                "accountingexportdatas");

        log.info(searchRequest.toString());
        SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

        final List<AccountingExportData> exportData = Arrays.stream(hits).map(SearchHit::getSourceAsString).map(s -> {
            try {
                return objectMapper.readValue(s,AccountingExportData.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        if (exportData.isEmpty()) {
            return new ArrayList<>();
        }
        return exportData;
    }

    public List<Order> findNotAccounted() throws Exception {
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("accounted"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb).build();

        SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                query,
                new SortBuilder[] {new FieldSortBuilder("_id").order(SortOrder.DESC)},
                "orders");

        log.info(searchRequest.toString());
        SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

        final List<Order> exportData = Arrays.stream(hits).map(SearchHit::getSourceAsString).map(s -> {
            try {
                return objectMapper.readValue(s,Order.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        if (exportData.isEmpty()) {
            return new ArrayList<>();
        }

        return exportData;
    }

}

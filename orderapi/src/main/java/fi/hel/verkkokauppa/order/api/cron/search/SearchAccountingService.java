package fi.hel.verkkokauppa.order.api.cron.search;

import fi.hel.verkkokauppa.common.elastic.search.QueryService;
import fi.hel.verkkokauppa.common.elastic.search.SearchService;
import fi.hel.verkkokauppa.order.model.accounting.OrderAccounting;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SearchAccountingService {

    @Autowired
    private SearchService searchService;

    @Autowired
    private QueryService queryService;

    public Set<String> getAccountedOrderIds(List<String> paidOrderIds) throws IOException {
        SearchSourceBuilder orderAccountingsQueryBuilder = new SearchSourceBuilder();
        BoolQueryBuilder orderAccountingsQuery = QueryBuilders.boolQuery();

        queryService.createOrderIdShouldQuery(paidOrderIds, orderAccountingsQuery);

        orderAccountingsQueryBuilder.query(orderAccountingsQuery);
        List<OrderAccounting> foundOrderAccountings = searchService.searchAcrossIndexes(
                List.of("orderaccountings"),
                orderAccountingsQueryBuilder,
                OrderAccounting.class
        );

        return foundOrderAccountings.stream()
                .map(OrderAccounting::getOrderId)
                .collect(Collectors.toSet());
    }
}

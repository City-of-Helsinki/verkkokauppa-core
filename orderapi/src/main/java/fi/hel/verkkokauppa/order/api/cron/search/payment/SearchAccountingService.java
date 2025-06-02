package fi.hel.verkkokauppa.order.api.cron.search.payment;

import fi.hel.verkkokauppa.common.elastic.search.QueryService;
import fi.hel.verkkokauppa.common.elastic.search.SearchService;
import fi.hel.verkkokauppa.order.model.accounting.OrderAccounting;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashSet;
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
        final int CHUNK_SIZE = 500;
        Set<String> accountedOrderIds = new HashSet<>();

        for (int i = 0; i < paidOrderIds.size(); i += CHUNK_SIZE) {
            int end = Math.min(paidOrderIds.size(), i + CHUNK_SIZE);
            List<String> chunk = paidOrderIds.subList(i, end);

            SearchSourceBuilder queryBuilder = new SearchSourceBuilder();
            BoolQueryBuilder query = QueryBuilders.boolQuery();

            queryService.createOrderIdShouldQuery(chunk, query);

            queryBuilder.query(query);
            List<OrderAccounting> results = searchService.searchAcrossIndexes(
                    List.of("orderaccountings"),
                    queryBuilder,
                    OrderAccounting.class
            );

            accountedOrderIds.addAll(
                    results.stream()
                            .map(OrderAccounting::getOrderId)
                            .collect(Collectors.toSet())
            );
        }

        return accountedOrderIds;
    }
}

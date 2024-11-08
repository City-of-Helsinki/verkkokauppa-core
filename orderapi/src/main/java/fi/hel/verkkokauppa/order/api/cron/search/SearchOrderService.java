package fi.hel.verkkokauppa.order.api.cron.search;

import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.common.elastic.search.SearchService;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class SearchOrderService {

    @Autowired
    private SearchService searchService;

    public List<Order> getUnaccountedOrders() throws IOException {
        SearchSourceBuilder ordersQueryBuilder = new SearchSourceBuilder();
        BoolQueryBuilder ordersQuery = QueryBuilders.boolQuery();

        ordersQuery.must(QueryBuilders.termQuery("status", "confirmed"));
        ordersQuery.must(QueryBuilders.rangeQuery("priceTotal").gt("0"));
        ordersQuery.mustNot(QueryBuilders.existsQuery("accounted"));

        ordersQueryBuilder.query(ordersQuery);
        return searchService.searchAcrossIndexes(
                List.of("orders"),
                ordersQueryBuilder,
                Order.class
        );
    }
}

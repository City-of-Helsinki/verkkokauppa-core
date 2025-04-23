package fi.hel.verkkokauppa.order.api.cron.search.refund;

import fi.hel.verkkokauppa.common.elastic.search.SearchService;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.refund.Refund;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SearchRefundService {

    @Autowired
    private SearchService searchService;

    public List<Refund> getUnaccountedRefunds() throws IOException {
        SearchSourceBuilder ordersQueryBuilder = new SearchSourceBuilder();
        BoolQueryBuilder ordersQuery = QueryBuilders.boolQuery();

        ordersQuery.must(QueryBuilders.termQuery("status", "confirmed"));
        ordersQuery.must(QueryBuilders.rangeQuery("priceTotal").gt("0"));
        ordersQuery.mustNot(QueryBuilders.existsQuery("accounted"));

        ordersQueryBuilder.query(ordersQuery);
        return searchService.searchAcrossIndexes(
                List.of("refunds"),
                ordersQueryBuilder,
                Refund.class
        );
    }

    public List<Refund> getUnaccountedRefunds(LocalDateTime createdAfter) throws IOException {
        // Define the European date-time format
        DateTimeFormatter europeanFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        // Log the date in the European format
        log.info("CreatedAfterDateTime: {}", createdAfter.format(europeanFormatter));

        SearchSourceBuilder ordersQueryBuilder = new SearchSourceBuilder();
        BoolQueryBuilder ordersQuery = QueryBuilders.boolQuery();

        ordersQuery.must(QueryBuilders.termQuery("status", "confirmed"));
        ordersQuery.must(QueryBuilders.rangeQuery("priceTotal").gt("0"));
        ordersQuery.mustNot(QueryBuilders.existsQuery("accounted"));

        ordersQueryBuilder.query(ordersQuery);
        List<Refund> orders = searchService.searchAcrossIndexes(
                List.of("refunds"),
                ordersQueryBuilder,
                Refund.class
        );

        return orders.stream()
                .filter(refund -> refund.getCreatedAt() != null && refund.getCreatedAt().isAfter(createdAfter))
                .collect(Collectors.toList());
    }

}

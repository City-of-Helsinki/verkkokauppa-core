package fi.hel.verkkokauppa.order.api.cron.search.refund;

import fi.hel.verkkokauppa.common.elastic.search.SearchService;
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

    public List<Refund> findUnaccountedRefunds() throws IOException {
        SearchSourceBuilder refundsQueryBuilder = new SearchSourceBuilder();
        BoolQueryBuilder refundsQuery = QueryBuilders.boolQuery();

        refundsQuery.must(QueryBuilders.termQuery("status", "confirmed"));
        refundsQuery.must(QueryBuilders.rangeQuery("priceTotal").gt("0"));
        refundsQuery.mustNot(QueryBuilders.existsQuery("accounted"));

        refundsQueryBuilder.query(refundsQuery);
        List<Refund> unaccountedRefunds = searchService.searchAcrossIndexes(
                List.of("refunds"),
                refundsQueryBuilder,
                Refund.class
        );
        log.info("Unaccounted refunds retrieved: {}", unaccountedRefunds.size());
        return unaccountedRefunds;
    }

    public List<Refund> findUnaccountedRefunds(LocalDateTime createdAfter) throws IOException {
        // Define the European date-time format
        DateTimeFormatter europeanFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        // Log the date in the European format
        log.info("CreatedAfterDateTime: {}", createdAfter.format(europeanFormatter));

        List<Refund> unaccountedRefunds = findUnaccountedRefunds();
        log.info("Unaccounted refunds retrieved: {}", unaccountedRefunds.size());
        return unaccountedRefunds.stream()
                .filter(refund -> refund.getCreatedAt() != null && refund.getCreatedAt().isAfter(createdAfter))
                .collect(Collectors.toList());
    }

}

package fi.hel.verkkokauppa.order.api.cron.search.renewal;

import fi.hel.verkkokauppa.common.elastic.search.SearchService;
import fi.hel.verkkokauppa.order.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class SearchRenewalService {

    @Autowired
    private SearchService searchService;


    public List<Order> getRenewalsToday(int hours, int minutes) throws IOException {

        SearchSourceBuilder renewalsQueryBuilder = new SearchSourceBuilder();
        BoolQueryBuilder renewalsQuery = QueryBuilders.boolQuery();
        
        LocalDateTime searchDateTime = LocalDate.now().atTime(hours, minutes);

        renewalsQuery.must(QueryBuilders.rangeQuery("createdAt").gte(searchDateTime));
        renewalsQuery.must(QueryBuilders.existsQuery("subscriptionId"));
        renewalsQuery.must(QueryBuilders.termQuery("type", "order"));

        renewalsQueryBuilder.query(renewalsQuery);
        return searchService.searchAcrossIndexes(
                List.of("orders"),
                renewalsQueryBuilder,
                Order.class
        );

    }

}

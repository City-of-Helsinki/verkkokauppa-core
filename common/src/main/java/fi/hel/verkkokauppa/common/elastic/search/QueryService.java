package fi.hel.verkkokauppa.common.elastic.search;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class QueryService {

    /**
     * Adds nested BoolQueries with match_phrase for each orderId in the provided array
     * to the given BoolQueryBuilder.
     *

     */
    public void createOrderIdShouldQuery(List<String> orderIds, BoolQueryBuilder mainQuery) {
        // Create a `BoolQueryBuilder` for the `should` clauses with each `orderId`
        BoolQueryBuilder filterBoolQuery = QueryBuilders.boolQuery()
                .minimumShouldMatch(1); // Ensure at least one `orderId` matches

        for (String orderId : orderIds) {
            BoolQueryBuilder innerShouldQuery = QueryBuilders.boolQuery()
                    .minimumShouldMatch(1)
                    .should(QueryBuilders.matchPhraseQuery("orderId", orderId));
            filterBoolQuery.should(innerShouldQuery);
        }


        // Add the filter `BoolQuery` to the main `BoolQuery`
        mainQuery.filter(filterBoolQuery);
    }
}

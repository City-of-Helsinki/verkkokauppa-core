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
     */
    public void createOrderIdShouldQuery(List<String> ids, BoolQueryBuilder mainQuery) {
        // Create a `BoolQueryBuilder` for the `should` clauses with each `fieldName`
        String fieldName = "orderId";
        addFilterBoolQueryByFieldName(ids, mainQuery, fieldName);
    }

    /**
     * Adds nested BoolQueries with match_phrase for each refundId in the provided array
     * to the given BoolQueryBuilder.
     */
    public void createRefundIdShouldQuery(List<String> ids, BoolQueryBuilder mainQuery) {
        // Create a `BoolQueryBuilder` for the `should` clauses with each `fieldName`
        String fieldName = "refundId";
        addFilterBoolQueryByFieldName(ids, mainQuery, fieldName);
    }

    private static void addFilterBoolQueryByFieldName(List<String> ids, BoolQueryBuilder mainQuery, String fieldName) {
        BoolQueryBuilder filterBoolQuery = QueryBuilders.boolQuery()
                .minimumShouldMatch(1); // Ensure at least one `fieldName` matches
        for (String id : ids) {
            BoolQueryBuilder innerShouldQuery = QueryBuilders.boolQuery()
                    .minimumShouldMatch(1)
                    .should(QueryBuilders.matchPhraseQuery(fieldName, id));
            filterBoolQuery.should(innerShouldQuery);
        }
        // Add the filter `BoolQuery` to the main `BoolQuery`
        mainQuery.filter(filterBoolQuery);
    }
}

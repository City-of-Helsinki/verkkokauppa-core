package fi.hel.verkkokauppa.order.service.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.accounting.AccountingExportData;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class SearchAfterService {

    @Autowired
    private RestHighLevelClient highLevelClient;

    @Value("${elasticsearch.search-after-page-size}")
    private int elasticsearchSearchAfterPageSize;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Build Json query based on NativeSearchQueryBuilder with added search after parameters.
     * <p>
     * For first pass businessSortMarker and tiebreakerSortMarker can be null,
     * for search after they are needed.
     */

    public SearchRequest buildSearchAfterSearchRequest(NativeSearchQuery query,
                                                       SortBuilder[] sorts,
                                                       String... indices) throws Exception {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        if (indices == null || indices.length == 0) {
            // throw exception, indice is needed
            throw new Exception("buildSearchAfterSearchRequest: at least one indice is needed");
        }

        if (query != null && !query.toString().isEmpty()) {
            // set query
            searchSourceBuilder.query(query.getQuery());
        }

        if (sorts != null) {
            // set sorts
            for (SortBuilder sort : sorts) {
                searchSourceBuilder.sort(sort);
            }
        }

        // set number of records retrieved with one query
        searchSourceBuilder.size(elasticsearchSearchAfterPageSize);

        return new SearchRequest().source(searchSourceBuilder).indices(indices);
    }


    public SearchHit[] executeSearchRequest(SearchRequest searchRequest) throws Exception {
        SearchHit[] resultHits;
        Object[] searchAfterSortValues;

        // First search for one page
        SearchResponse response = this.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits searchHits = response.getHits();
        resultHits = searchHits.getHits();

        // if we got full page do another search just in case
        if (resultHits.length == elasticsearchSearchAfterPageSize) {
            // add search after parameters to search request
            searchAfterSortValues = resultHits[resultHits.length - 1].getSortValues();
            // can only do search after if we get sort values
            if (searchAfterSortValues != null && searchAfterSortValues.length > 0) {

                searchRequest.source().searchAfter(searchAfterSortValues);

                // call this method to do another search
                final SearchHit[] hits = executeSearchRequest(searchRequest);

                // add new hits to results
                resultHits = Stream.concat(Arrays.stream(resultHits), Arrays.stream(hits))
                        .toArray(size -> (SearchHit[]) Array.newInstance(hits.getClass().getComponentType(), size));
            }
        }

        return resultHits;
    }

    public SearchResponse search(SearchRequest searchRequest, RequestOptions options) throws Exception {
        if (searchRequest.indices() == null || searchRequest.indices().length == 0) {
            throw new Exception("SearchRequest should have at least one indice.");
        }

        return highLevelClient.search(searchRequest, options);
    }

    public SortBuilder[] buildSortWithId() {
        SortBuilder[] sortBuilder = new SortBuilder[]{
                new FieldSortBuilder("_id")
                        .order(SortOrder.DESC)
        };
        return sortBuilder;
    }

    public <T> List<T> buildListFromHits(SearchHit[] hits, Class<T> objectClass) {
        final List<T> exportData = Arrays.stream(hits).map(SearchHit::getSourceAsString).map(s -> {
            try {
                return objectMapper.readValue(s, objectClass);
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

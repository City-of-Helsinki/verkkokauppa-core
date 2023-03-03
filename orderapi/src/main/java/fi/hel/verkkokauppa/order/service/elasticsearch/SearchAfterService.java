package fi.hel.verkkokauppa.order.service.elasticsearch;



import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.naming.directory.SearchResult;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.springframework.util.ObjectUtils.isEmpty;

@Service
@Slf4j
public class SearchAfterService {

    @Autowired
    private RestHighLevelClient highLevelClient;

    @Autowired
    private Environment env;

    /**
     * Build Json query based on NativeSearchQueryBuilder with added search after parameters.
     *
     * For first pass businessSortMarker and tiebreakerSortMarker can be null,
     * for search after they are needed.
     */

    public SearchRequest buildSearchAfterSearchRequest(String indices,
                                                       NativeSearchQuery query,
                                                       SortBuilder[] sorts) throws Exception {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        if( query != null && !query.toString().isEmpty() )
        {
            // set query
            searchSourceBuilder.query(query.getQuery());
        }

        if( sorts != null )
        {
            // set sorts
            for (SortBuilder sort : sorts) {
                searchSourceBuilder.sort(sort);
            }
        }

        // set number of records retrieved with one query
        searchSourceBuilder.size(Integer.parseInt(env.getProperty("elasticsearch.search-after-page-size")));

        return new SearchRequest().source(searchSourceBuilder).indices(indices);
    }


    public SearchHit[] executeSearchRequest(SearchRequest searchRequest) throws IOException {
        SearchHit[] resultHits;
        Object[] searchAfterSortValues;
        int searchAfterPageSize = Integer.parseInt(env.getProperty("elasticsearch.search-after-page-size"));

        // First search for one page
        SearchResponse response = highLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits searchHits = response.getHits();
        resultHits = searchHits.getHits();

        // if we got full page do another search just in case
        if( resultHits.length == searchAfterPageSize ){
            // add search after parameters to search request
            searchAfterSortValues = resultHits[resultHits.length-1].getSortValues();
            // can only do search after if we get sort values
            if( searchAfterSortValues != null && searchAfterSortValues.length > 0 ) {

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





}

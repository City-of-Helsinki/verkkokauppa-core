package fi.hel.verkkokauppa.common.elastic.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.common.elastic.ElasticSearchRestClientResolver;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
@Slf4j
public class SearchService {

    private final RestHighLevelClient client;
    private final ObjectMapper objectMapper;

    @Autowired
    public SearchService(ElasticSearchRestClientResolver clientResolver, ObjectMapper objectMapper) {
        this.client = clientResolver.get();
        this.objectMapper = objectMapper;
    }

    public <Dto> List<Dto> searchAcrossIndexes(
            List<String> indexes,
            SearchSourceBuilder searchSourceBuilder,
            Class<Dto> dtoClass
    ) throws IOException {
        List<Dto> results = new ArrayList<>();
        int from = 0;
        // TODO
        int pageSize = 10000;
        int hitsLength;

        // Convert List<String> to String[] for use in SearchRequest
        String[] indexesArray = indexes.toArray(new String[0]);

        do {
            // Set `from` and `size` for pagination
            searchSourceBuilder.from(from);
            searchSourceBuilder.size(pageSize);

            // Create a new SearchRequest for each iteration with updated `from` and `size`
            SearchRequest searchRequest = new SearchRequest(indexesArray);
            searchRequest.source(searchSourceBuilder);

            // Log the generated query JSON for debugging
//            log.info("Generated Query for searchAcrossIndexes: {}", searchSourceBuilder.toString());

            // Execute the search request
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = searchResponse.getHits();
            hitsLength = hits.getHits().length;

            // Inline `SearchResultMapper` to map results to DTOs
            SearchResultMapper<Map<String, Object>, Dto> searchResultMapper = new SearchResultMapper<>(
                    objectMapper,
                    HashMap::new,
                    () -> {
                        try {
                            return dtoClass.getDeclaredConstructor().newInstance();
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to create DTO instance", e);
                        }
                    }
            );

            // Convert search hits to DTOs using the inlined `SearchResultMapper`
            for (SearchHit hit : hits) {
                Map<String, Object> sourceMap = hit.getSourceAsMap();
                Dto dto = searchResultMapper.toDto(sourceMap); // Convert each hit to DTO
                results.add(dto);
            }

            // Increment `from` to get the next page
            from += pageSize;

        } while (hitsLength == pageSize); // Continue while the last page had `pageSize` results

        return results;
    }
}

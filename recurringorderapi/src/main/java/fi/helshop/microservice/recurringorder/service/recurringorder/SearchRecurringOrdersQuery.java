package fi.helshop.microservice.recurringorder.service.recurringorder;

import com.alibaba.fastjson.JSON;
import fi.helshop.microservice.recurringorder.model.recurringorder.RecurringOrder;
import fi.helshop.microservice.recurringorder.rest.model.RecurringOrderDto;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchRecurringOrdersQuery {

	private final RestHighLevelClient elasticsearchClient;

	@Autowired
	public SearchRecurringOrdersQuery(
			RestHighLevelClient elasticsearchClient
	) {
		this.elasticsearchClient = elasticsearchClient;
	}

	public List<RecurringOrderDto> searchActiveForDateRange(LocalDate date) throws IOException {
		BoolQueryBuilder qb = QueryBuilders.boolQuery();

		QueryBuilder rangeQuery = QueryBuilders.rangeQuery("field").gte(date); // TODO: ok?
		qb.should(QueryBuilders.boolQuery().should(rangeQuery));

		qb.must(QueryBuilders.termQuery("status", RecurringOrder.Status.ACTIVE)); // TODO: ok?

		SearchRequest searchRequest = new SearchRequest();
		SearchResponse response = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
		SearchHit[] searchHits = response.getHits().getHits();

		return Arrays.stream(searchHits)
				.map(hit -> JSON.parseObject(hit.getSourceAsString(), RecurringOrderDto.class))
				.collect(Collectors.toList());
	}

	// TODO: (customer id, 2 x address id, merchant namespace?)
}

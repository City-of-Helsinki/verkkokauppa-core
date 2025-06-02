package fi.hel.verkkokauppa.order.api.cron.search.refund;

import fi.hel.verkkokauppa.common.elastic.search.QueryService;
import fi.hel.verkkokauppa.common.elastic.search.SearchService;
import fi.hel.verkkokauppa.order.api.cron.search.dto.RefundResultDto;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SearchRefundPaymentService {

    @Autowired
    private SearchService searchService;

    @Autowired
    private QueryService queryService;

    public List<RefundResultDto> findRefundsByStatusAndOrderIds(List<String> orderIds, String refundStatus) throws IOException {
        final int CHUNK_SIZE = 1;
        List<RefundResultDto> allResults = new ArrayList<>();

        for (int i = 0; i < orderIds.size(); i += CHUNK_SIZE) {
            int end = Math.min(orderIds.size(), i + CHUNK_SIZE);
            List<String> chunk = orderIds.subList(i, end);

            SearchSourceBuilder queryBuilder = new SearchSourceBuilder();
            BoolQueryBuilder query = QueryBuilders.boolQuery();

            query.must(QueryBuilders.termQuery("status", refundStatus));
            queryService.createOrderIdShouldQuery(chunk, query);

            queryBuilder.query(query);
            List<RefundResultDto> results = searchService.searchAcrossIndexes(
                    List.of("refund_payments"),
                    queryBuilder,
                    RefundResultDto.class
            );
            allResults.addAll(results);
        }

        return allResults;
    }
}

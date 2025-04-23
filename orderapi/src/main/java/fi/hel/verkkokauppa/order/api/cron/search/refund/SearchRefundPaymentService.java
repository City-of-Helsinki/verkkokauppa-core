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
import java.util.List;

@Service
public class SearchRefundPaymentService {

    @Autowired
    private SearchService searchService;

    @Autowired
    private QueryService queryService;

    public List<RefundResultDto> findRefundsByStatusAndOrderIds(List<String> orderIds, String refundStatus) throws IOException {
        SearchSourceBuilder paymentsQueryBuilder = new SearchSourceBuilder();
        BoolQueryBuilder paymentsQuery = QueryBuilders.boolQuery();

        paymentsQuery.must(QueryBuilders.termQuery("status", refundStatus));
        queryService.createOrderIdShouldQuery(orderIds, paymentsQuery);

        paymentsQueryBuilder.query(paymentsQuery);
        return searchService.searchAcrossIndexes(
                List.of("refunds"),
                paymentsQueryBuilder,
                RefundResultDto.class
        );
    }
}

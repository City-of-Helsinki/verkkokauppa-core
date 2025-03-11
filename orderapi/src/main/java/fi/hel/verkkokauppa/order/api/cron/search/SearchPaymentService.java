package fi.hel.verkkokauppa.order.api.cron.search;

import fi.hel.verkkokauppa.common.elastic.search.QueryService;
import fi.hel.verkkokauppa.common.elastic.search.SearchService;
import fi.hel.verkkokauppa.order.api.cron.search.dto.PaymentResultDto;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class SearchPaymentService {

    @Autowired
    private SearchService searchService;

    @Autowired
    private QueryService queryService;

    public List<PaymentResultDto> findPaymentsByStatusAndOrderIds(List<String> orderIds, String paymentStatus) throws IOException {
        SearchSourceBuilder paymentsQueryBuilder = new SearchSourceBuilder();
        BoolQueryBuilder paymentsQuery = QueryBuilders.boolQuery();

        paymentsQuery.must(QueryBuilders.termQuery("status", paymentStatus));
        queryService.createOrderIdShouldQuery(orderIds, paymentsQuery);

        paymentsQueryBuilder.query(paymentsQuery);
        return searchService.searchAcrossIndexes(
                List.of("payments"),
                paymentsQueryBuilder,
                PaymentResultDto.class
        );
    }
}

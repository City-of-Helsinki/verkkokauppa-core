package fi.hel.verkkokauppa.order.api.cron.search.refund;

import fi.hel.verkkokauppa.common.elastic.search.QueryService;
import fi.hel.verkkokauppa.common.elastic.search.SearchService;
import fi.hel.verkkokauppa.order.model.accounting.OrderAccounting;
import fi.hel.verkkokauppa.order.model.accounting.RefundAccounting;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SearchRefundAccountingService {

    @Autowired
    private SearchService searchService;

    @Autowired
    private QueryService queryService;

    public Set<String> getAccountedOrderIds(List<String> refundedOrderIds) throws IOException {
        SearchSourceBuilder refundAccountingsQueryBuilder = new SearchSourceBuilder();
        BoolQueryBuilder refundAccountingsQuery = QueryBuilders.boolQuery();

        queryService.createOrderIdShouldQuery(refundedOrderIds, refundAccountingsQuery);

        refundAccountingsQueryBuilder.query(refundAccountingsQuery);
        List<RefundAccounting> foundRefundAccountings = searchService.searchAcrossIndexes(
                List.of("refund_accountings"),
                refundAccountingsQueryBuilder,
                RefundAccounting.class
        );

        return foundRefundAccountings.stream()
                .map(RefundAccounting::getOrderId)
                .collect(Collectors.toSet());
    }
}

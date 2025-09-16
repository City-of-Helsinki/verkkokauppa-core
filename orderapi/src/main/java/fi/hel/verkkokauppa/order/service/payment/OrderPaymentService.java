package fi.hel.verkkokauppa.order.service.payment;

import fi.hel.verkkokauppa.common.elastic.search.QueryService;
import fi.hel.verkkokauppa.common.elastic.search.SearchService;
import fi.hel.verkkokauppa.order.api.cron.search.dto.PaymentResultDto;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderPaymentService {

    @Autowired
    private SearchService searchService;

    @Autowired
    private QueryService queryService;

    public PaymentResultDto findPaymentForOrder(String orderId) throws IOException {
        // find all payments for order
        SearchSourceBuilder paymentsQueryBuilder = new SearchSourceBuilder();
        BoolQueryBuilder paymentsQuery = QueryBuilders.boolQuery();

        paymentsQuery.must(QueryBuilders.matchPhraseQuery("orderId", orderId));

        paymentsQueryBuilder.query(paymentsQuery);
        List<PaymentResultDto> results = searchService.searchAcrossIndexes(
                List.of("payments"),
                paymentsQueryBuilder,
                PaymentResultDto.class
        );

        // if there are no payments return null
        if(results == null || results.isEmpty()) {
            return null;
        }

        // Sort newest to be first
        results.sort(Comparator.comparing(PaymentResultDto::getCreatedAt).reversed());

        // Find all items with status = "done"
        List<PaymentResultDto> paidPayments = results.stream()
                .filter(payment -> "payment_paid_online".equals(payment.getStatus()))
                .collect(Collectors.toList());

        // if there are paid payments then return the first one
        if( paidPayments != null && paidPayments.size() > 0 ) {
            return paidPayments.get(0);
        }

        // otherwise return the latest payment
        return results.get(0);
    }
}

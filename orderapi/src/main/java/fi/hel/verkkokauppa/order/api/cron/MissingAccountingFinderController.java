package fi.hel.verkkokauppa.order.api.cron;

import fi.hel.verkkokauppa.common.elastic.search.QueryService;
import fi.hel.verkkokauppa.common.elastic.search.SearchService;
import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.accounting.OrderAccounting;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
@RestController
public class MissingAccountingFinderController {

    private Logger log = LoggerFactory.getLogger(MissingAccountingFinderController.class);

    @Autowired
    private SearchService searchService;
    @Autowired
    private QueryService queryService;

    @GetMapping(value = "/accounting/find-missing-accounting", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> findMissingAccountingsBasedOnPaymentPaid() {
        // TODO
        List<String> failedToAccount = new ArrayList<>();
        try {
            SearchSourceBuilder ordersQueryBuilder = new SearchSourceBuilder();
            BoolQueryBuilder ordersQuery = QueryBuilders.boolQuery();

            // Add filter for orders where status is "confirmed"
            ordersQuery.must(QueryBuilders.termQuery("status", "confirmed"));

            // Price total needs to be more than 0
            ordersQuery.must(QueryBuilders.rangeQuery("priceTotal").gt("0"));

            // Exclude documents where "accounted" field exists
            ordersQuery.mustNot(QueryBuilders.existsQuery("accounted"));

            // Set the BoolQueryBuilder as the query in SearchSourceBuilder
            ordersQueryBuilder.query(ordersQuery);

            // Execute the search to get unaccounted orders
            List<Order> unaccountedOrders = searchService.searchAcrossIndexes(
                    List.of("orders"),
                    ordersQueryBuilder,
                    Order.class
            );

            // Extract orderIds from the unaccounted orders
            List<String> orderIds = unaccountedOrders.stream()
                    .map(Order::getOrderId)
                    .collect(Collectors.toList());
            // Check if there are any orderIds to query
            if (!orderIds.isEmpty()) {
                SearchSourceBuilder paymentsQueryBuilder = new SearchSourceBuilder();
                BoolQueryBuilder paymentsQuery = QueryBuilders.boolQuery();

                // Filter for payments with status "payment_paid_online"
                paymentsQuery.must(QueryBuilders.termQuery("status", "payment_paid_online"));

                // Filter only given orderIds to be returned in query
                this.queryService.createOrderIdShouldQuery(orderIds, paymentsQuery);

                // Set the main `BoolQuery` in the SearchSourceBuilder
                paymentsQueryBuilder.query(paymentsQuery);

                // Execute the search to get matching payments
                List<PaymentResultDto> matchedPayments = searchService.searchAcrossIndexes(
                        List.of("payments"),
                        paymentsQueryBuilder,
                        PaymentResultDto.class
                );

                // Collect `orderId`s from matched payments
                List<String> paidOrderIds = matchedPayments.stream()
                        .map(PaymentResultDto::getOrderId)
                        .collect(Collectors.toList());

                log.info(String.valueOf(matchedPayments.size()));


                if (!paidOrderIds.isEmpty()) {
                    SearchSourceBuilder orderAccountingsQueryBuilder = new SearchSourceBuilder();
                    BoolQueryBuilder orderAccountingsQuery = QueryBuilders.boolQuery();

                    this.queryService.createOrderIdShouldQuery(paidOrderIds,orderAccountingsQuery);

                    // Set the main `BoolQuery` in the SearchSourceBuilder
                    orderAccountingsQueryBuilder.query(orderAccountingsQuery);
                    // Execute the search to get matching accountings
                    List<OrderAccounting> foundOrderAccountings = searchService.searchAcrossIndexes(
                            List.of("orderaccountings"),
                            orderAccountingsQueryBuilder,
                            OrderAccounting.class
                    );
                    // Collect `orderId`s from found order accountings
                    Set<String> accountedOrderIds = foundOrderAccountings.stream()
                            .map(OrderAccounting::getOrderId)
                            .collect(Collectors.toCollection(HashSet::new));

                    // Filter out `paidOrderIds` that are present in `accountedOrderIds`
                    List<String> unaccountedOrderIds = paidOrderIds.stream()
                            .filter(orderId -> !accountedOrderIds.contains(orderId))
                            .collect(Collectors.toList());

                    log.info(String.valueOf(unaccountedOrderIds.size()));
                    log.info("Unaccounted order IDs: {}", unaccountedOrderIds);
                    // Add to failed to account list
                    failedToAccount.addAll(unaccountedOrderIds);
                }
            }


            return ResponseEntity.ok().body(failedToAccount);
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("creating accounting data failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-create-accounting-data", "failed to create accounting data")
            );
        }
    }

    private static void createOrderIdShouldQuery(List<String> orderIds, BoolQueryBuilder paymentsQuery) {
        // Create a `BoolQueryBuilder` for the `should` clauses with each `orderId`
        BoolQueryBuilder filterBoolQuery = QueryBuilders.boolQuery()
                .minimumShouldMatch(1); // Ensure at least one `orderId` matches

        for (String orderId : orderIds) {
            BoolQueryBuilder innerShouldQuery = QueryBuilders.boolQuery()
                    .minimumShouldMatch(1)
                    .should(QueryBuilders.matchPhraseQuery("orderId", orderId));
            filterBoolQuery.should(innerShouldQuery);
        }


        // Add the filter `BoolQuery` to the main `BoolQuery`
        paymentsQuery.filter(filterBoolQuery);
    }

}

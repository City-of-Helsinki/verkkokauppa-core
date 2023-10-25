package fi.hel.verkkokauppa.order.api.admin;

import fi.hel.verkkokauppa.common.error.CommonApiException;
import fi.hel.verkkokauppa.common.error.Error;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.OrderItemDto;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.accounting.OrderAccounting;
import fi.hel.verkkokauppa.order.model.accounting.OrderItemAccounting;
import fi.hel.verkkokauppa.order.repository.jpa.OrderAccountingRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderItemAccountingRepository;
import fi.hel.verkkokauppa.order.service.accounting.OrderItemAccountingService;
import fi.hel.verkkokauppa.order.service.elasticsearch.SearchAfterService;
import fi.hel.verkkokauppa.order.service.order.OrderService;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class AccountingAdminController {

    private Logger log = LoggerFactory.getLogger(AccountingAdminController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderItemAccountingService orderItemAccountingService;

    @Autowired
    private OrderItemAccountingRepository orderItemAccountingRepository;

    @Autowired
    private SearchAfterService searchAfterService;

    @Autowired
    private OrderAccountingRepository orderAccountingRepository;

    @PostMapping(value = "/accounting-admin/copyAccountedDates")
    public ResponseEntity<Void> copyAccountedDates() {
        try {
            NativeSearchQuery query = new NativeSearchQueryBuilder().withQuery(QueryBuilders.matchAllQuery()).build();
            SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                    query,
                    searchAfterService.buildSortWithId(),
                    OrderAccounting.INDEX_NAME
            );
            log.info(searchRequest.toString());

            SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);
            final List<OrderAccounting> orderAccountings = searchAfterService.buildListFromHits(hits, OrderAccounting.class);

            for (OrderAccounting orderAccounting : orderAccountings) {
                Order order = orderService.findById(orderAccounting.getOrderId());
                LocalDate accounted = order == null ? LocalDate.EPOCH : order.getAccounted();
                orderAccounting.setAccounted(accounted);
                orderAccountingRepository.save(orderAccounting);
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("copying accounted dates failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-copy-order-accounting-accounted", "failed to copy order accounting accounted dates")
            );
        }
    }

    @GetMapping(value = "/accounting-admin/updateOrderAccountingVatPrices", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> updateOrderAccountingVatPrices() {
        try {
            List<String> updatedAccountings = new ArrayList<>();

            List<OrderItemAccounting> orderItemAccountings = orderItemAccountingService.getOrderItemAccountings();

            for (OrderItemAccounting orderItemAccounting : orderItemAccountings) {
                if (orderItemAccounting.getPriceVat() == null) {
                    String orderId = orderItemAccounting.getOrderId();
                    String orderItemId = orderItemAccounting.getOrderItemId();

                    OrderAggregateDto orderWithItems = orderService.getOrderWithItems(orderId);
                    List<OrderItemDto> items = orderWithItems.getItems();

                    for (OrderItemDto item : items) {
                        if (item.getOrderItemId().equalsIgnoreCase(orderItemId)) {
                            orderItemAccounting.setPriceVat(item.getPriceVat());

                            orderItemAccountingRepository.save(orderItemAccounting);
                            updatedAccountings.add(orderId);

                            log.info("Updated order accounting vat price for order id: [" + orderId + "], order item id: [" + orderItemId + "]");
                        }
                    }

                }

            }

            log.info("Updated order accounting vat prices");
            return ResponseEntity.ok().body(updatedAccountings.stream().distinct().collect(Collectors.toList()));
        } catch (CommonApiException cae) {
            throw cae;
        } catch (Exception e) {
            log.error("updating order accounting vat prices failed", e);
            throw new CommonApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    new Error("failed-to-update-order-accounting-vat-prices", "failed to update order accounting vat prices")
            );
        }
    }

}

package fi.hel.verkkokauppa.order.test.utils;

import fi.hel.verkkokauppa.common.rest.refund.RefundAggregateDto;
import fi.hel.verkkokauppa.common.rest.refund.RefundDto;
import fi.hel.verkkokauppa.common.rest.refund.RefundItemDto;
import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.OrderItemDto;
import fi.hel.verkkokauppa.order.api.data.OrderItemMetaDto;
import fi.hel.verkkokauppa.order.api.data.OrderPaymentMethodDto;
import fi.hel.verkkokauppa.order.api.data.accounting.AccountingExportDataDto;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.accounting.AccountingExportData;
import fi.hel.verkkokauppa.order.model.accounting.OrderAccounting;
import fi.hel.verkkokauppa.order.model.refund.Refund;
import fi.hel.verkkokauppa.order.repository.jpa.*;
import fi.hel.verkkokauppa.order.service.accounting.AccountingExportDataService;
import fi.hel.verkkokauppa.order.service.accounting.AccountingSearchService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SearchAfterServiceTestUtils extends TestUtils {

    @Autowired
    AccountingSearchService searchServiceToTest;

    @Autowired
    AccountingExportDataService accountingExportDataService;

    @Autowired
    private ElasticsearchOperations operations;

    @Autowired
    private AccountingExportDataRepository exportDataRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private RefundItemRepository refundItemRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderItemMetaRepository orderItemMetaRepository;

    private ArrayList<String> toBeDeletedOrderById = new ArrayList<>();
    private ArrayList<String> toBeDeletedOrderItemById = new ArrayList<>();
    private ArrayList<String> toBeDeletedOrderItemMetaById = new ArrayList<>();
    private ArrayList<String> toBeDeletedRefundById = new ArrayList<>();
    private ArrayList<String> toBeDeletedRefundItemById = new ArrayList<>();
    private ArrayList<String> toBeDeletedAccountingById = new ArrayList<>();


    // test utility method for creating accounting export data to Elasticsearch
    // usable for local testing of services
    public void createAccountingExportData(long testRecordsCount) {

        // if there are less than wanted amount of accountingexportdatas records then create more
        long actualDataCount = accountingExportDataCount();
        log.info("Number of existing test data rows " + actualDataCount);
        if (actualDataCount < testRecordsCount) {
            long newRecordCount = testRecordsCount - actualDataCount;
            log.info("Creating " + newRecordCount + " more test records for accountingexportdatas");

            for (long i = 0; i < newRecordCount; i++) {
                String uuid = UUID.randomUUID().toString() + "-testaccounting";
                toBeDeletedAccountingById.add(uuid);
                accountingExportDataService.createAccountingExportData(
                        new AccountingExportDataDto(uuid, LocalDate.now(), "Some text " + i)
                );
            }

        }
    }

    // test utility method for removing all created accounting export data to Elasticsearch
    // usable for local testing of services
    public void clearAccountingExportData() {

        for (String id : toBeDeletedAccountingById) {
            exportDataRepository.deleteById(id);
        }
        toBeDeletedAccountingById = new ArrayList<>();

    }

    // returns number of AccountingExportData records in Elasticsearch
    public long accountingExportDataCount() {

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withPageable(PageRequest.of(0, 10000))
                .build();

        SearchHits<AccountingExportData> hits = operations.search(query, AccountingExportData.class);
        return hits.getTotalHits();
    }
    // returns the count of accountingExportDatas that have not been exported
    public long notExportedAccountingExportDataCount() {
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("exported"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb)
                .withPageable(PageRequest.of(0, 10000))
                .build();

        SearchHits<AccountingExportData> hits = operations.search(query, AccountingExportData.class);
        return hits.getTotalHits();
    }

    // test utility method for creating orders that have not been accounted
    // usable for local testing of services
    public void createOrder(long testRecordsCount) {

        // if there are less than wanted amount of order records then create more
        long actualDataCount = notAccountedOrderCount();
        log.info("Number of existing test data rows " + actualDataCount);
        if (actualDataCount < testRecordsCount) {
            long newRecordCount = testRecordsCount - actualDataCount;
            log.info("Creating " + newRecordCount + " more test records for accountingexportdatas");

            for (long i = 0; i < newRecordCount; i++) {
                ResponseEntity<OrderAggregateDto> response = createNewOrderToDatabase(1);
                toBeDeletedOrderById.add(response.getBody().getOrder().getOrderId());
                for (OrderItemDto item: response.getBody().getItems()) {
                    toBeDeletedOrderItemById.add(item.getOrderItemId());
                    for (OrderItemMetaDto meta: item.getMeta()) {
                        toBeDeletedOrderItemMetaById.add(meta.getOrderItemId());
                    }
                }
            }
        }
    }

    // deletes all created order records
    public void deleteNotAccountedOrders() {
        for (String id : toBeDeletedOrderById) {
            orderRepository.deleteById(id);
        }
        toBeDeletedOrderById = new ArrayList<>();

        for (String id : toBeDeletedOrderItemById) {
            orderItemRepository.deleteById(id);
        }
        toBeDeletedOrderItemById = new ArrayList<>();

        for (String id : toBeDeletedOrderItemMetaById) {
            orderItemMetaRepository.deleteById(id);
        }
        toBeDeletedOrderItemMetaById = new ArrayList<>();
    }

    // returns number of order records that have not been Accounted
    public long notAccountedOrderCount() {
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("accounted"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb)
                .withPageable(PageRequest.of(0, 10000))
                .build();
        SearchHits<Order> hits = operations.search(query, Order.class);

        return hits.getTotalHits();
    }

    public long notAccountedOrderAccountingCount() {
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("accounted"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb)
                .withPageable(PageRequest.of(0, 10000))
                .build();
        SearchHits<OrderAccounting> hits = operations.search(query, OrderAccounting.class);

        return hits.getTotalHits();
    }

    // returns number of order records
    public long orderCount() {
        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withPageable(PageRequest.of(0, 10000))
                .build();
        SearchHits<Order> hits = operations.search(query, Order.class);

        return hits.getTotalHits();
    }

    // test utility method for creating refunds that have not been accounted
    // usable for local testing of services
    public void createRefund(long testRecordsCount) {

        // if there are less than wanted amount of order records then create more
        long actualDataCount = notAccountedRefundCount();
        log.info("Number of existing test data rows " + actualDataCount);
        if (actualDataCount < testRecordsCount) {
            long newRecordCount = testRecordsCount - actualDataCount;
            log.info("Creating " + newRecordCount + " more test records for accountingexportdatas");

            for (long i = 0; i < newRecordCount; i++) {
                ResponseEntity<RefundAggregateDto> response = createNewRefundToDatabase(1, "testOrderId");
                toBeDeletedRefundById.add(response.getBody().getRefund().getRefundId());
                for (RefundItemDto item: response.getBody().getItems()) {
                    toBeDeletedRefundItemById.add(item.getRefundItemId());
                }
            }
        }
    }

    // deletes all created refund records
    public void deleteNotAccountedRefunds() {
        for (String id : toBeDeletedRefundById) {
            refundRepository.deleteById(id);
        }
        toBeDeletedRefundById = new ArrayList<>();

        for (String id : toBeDeletedRefundItemById) {
            refundItemRepository.deleteById(id);
        }
        toBeDeletedRefundItemById = new ArrayList<>();
    }


    // returns number of refund records that have not been Accounted
    public long notAccountedRefundCount() {
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("accounted"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb)
                .withPageable(PageRequest.of(0, 10000))
                .build();
        SearchHits<Refund> hits = operations.search(query, Refund.class);

        return hits.getTotalHits();
    }


}

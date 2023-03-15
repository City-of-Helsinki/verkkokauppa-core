package fi.hel.verkkokauppa.order.test.utils;

import fi.hel.verkkokauppa.order.api.data.OrderAggregateDto;
import fi.hel.verkkokauppa.order.api.data.OrderPaymentMethodDto;
import fi.hel.verkkokauppa.order.api.data.accounting.AccountingExportDataDto;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.accounting.AccountingExportData;
import fi.hel.verkkokauppa.order.repository.jpa.AccountingExportDataRepository;
import fi.hel.verkkokauppa.order.repository.jpa.OrderRepository;
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

    private ArrayList<String> toBeDeletedOrderById = new ArrayList<>();
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
            }
        }
    }

    // deletes all created order records
    public void deleteNotAccountedOrders() {
        for (String id : toBeDeletedOrderById) {
            orderRepository.deleteById(id);
        }
        toBeDeletedOrderById = new ArrayList<>();
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


}

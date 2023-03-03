package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.common.id.IncrementId;
import fi.hel.verkkokauppa.common.util.DateTimeUtil;
import fi.hel.verkkokauppa.order.api.data.accounting.AccountingExportDataDto;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.accounting.AccountingExportData;
import fi.hel.verkkokauppa.order.service.elasticsearch.SearchAfterService;
import fi.hel.verkkokauppa.order.test.utils.TestUtils;
import fi.hel.verkkokauppa.order.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


@RunWith(SpringJUnit4ClassRunner.class )
@SpringBootTest
@RunIfProfile(profile = "local")
@TestPropertySource(properties = { "elasticsearch.search-after-page-size = 2000" })
@ComponentScan("fi.hel.verkkokauppa.order.service.elasticsearch")
@Slf4j
public class AccountingSearchServiceTest {

    @Autowired
    private SearchAfterService searchAfterService;

    @Autowired
    private TestUtils testUtils;

    @Autowired
    private Environment env;

    @Autowired
    AccountingSearchService searchServiceToTest;

    @Autowired
    AccountingExportDataService accountingExportDataService;

    @Autowired
    private ElasticsearchOperations operations;

    @Before
    public void initTests(){
        // creates test data to Elasticsearch if none exists
        createAccountingExportData(10);
        createOrder(10);
    }



    @Test
    public void whenGetNotExportedAccountingExportData_thenSuccess() throws Exception {
        log.info("running whenGetNotExportedAccountingExportData_thenSuccess");
        List<AccountingExportData> resultList;
        long expectedTotalHits = accountingExportDataCount();

        try{
            log.info("elasticsearch.search-after-page-size: " + env.getProperty("elasticsearch.search-after-page-size"));
            resultList = searchServiceToTest.getNotExportedAccountingExportData();
            log.info("Result list size: "+resultList.size());

            assertFalse("Result list should not be empty",resultList.isEmpty());

            // maximum number of total hits we can receive is 10 000. Cannot verify counts with 10000 or more
            if(expectedTotalHits < 10000) {
                assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", expectedTotalHits, resultList.size());
            }

        }
        catch (Exception e){
            assertFalse("Exception: " + e.getMessage(),true);
        }
    }

    @Test
    public void whenFindNotAccounted_thenSuccess() throws Exception {
        log.info("running whenFindNotAccounted_thenSuccess");
        List<Order> resultList;
        long expectedTotalHits = notAccountedOrderCount();

        try{
            log.info("elasticsearch.search-after-page-size: " + env.getProperty("elasticsearch.search-after-page-size"));
            resultList = searchServiceToTest.findNotAccounted();
            log.info("Result list size: "+resultList.size());

            assertFalse("Result list should not be empty",resultList.isEmpty());

            // maximum number of total hits we can receive is 10 000. Cannot verify counts with 10000 or more
            if(expectedTotalHits < 10000) {
                assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", expectedTotalHits, resultList.size());
            }

        }
        catch (Exception e){
            assertFalse("Exception: " + e.getMessage(),true);
        }
    }

    // test utility method for creating  accounting export data to Elasticsearch
    // usable for local testing of services
    private void createAccountingExportData (long testRecordsCount){

        // if there are less than wanted amount of accountingexportdatas records then create more
        long actualDataCount = accountingExportDataCount();
        log.info("Number of existing test data rows " + actualDataCount);
        if ( actualDataCount < testRecordsCount ) {
            long newRecordCount = testRecordsCount-actualDataCount;
            log.info("Creating " + newRecordCount + " more test records for accountingexportdatas");

            for(long i=0; i < newRecordCount; i++) {
                accountingExportDataService.createAccountingExportData(
                        new AccountingExportDataDto(UUID.randomUUID().toString()+"-testaccounting", LocalDate.now(), "Some text " + i)
                );
            }

        }
    }

    // returns number of AccountingExportData records in Elasticsearch
    private long accountingExportDataCount(){
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
    private void createOrder (long testRecordsCount){

        // if there are less than wanted amount of order records then create more
        long actualDataCount = notAccountedOrderCount();
        log.info("Number of existing test data rows " + actualDataCount);
        if ( actualDataCount < testRecordsCount ) {
            long newRecordCount = testRecordsCount-actualDataCount;
            log.info("Creating " + newRecordCount + " more test records for accountingexportdatas");

            for(long i=0; i < newRecordCount; i++) {
                    testUtils.createNewOrderToDatabase(1);
            }
        }
    }

    // returns number of order records that have not been Accounted
    private long notAccountedOrderCount(){
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("accounted"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb)
                .withPageable(PageRequest.of(0, 10000))
                .build();
        SearchHits<Order> hits = operations.search(query, Order.class);

        return hits.getTotalHits();
    }


}

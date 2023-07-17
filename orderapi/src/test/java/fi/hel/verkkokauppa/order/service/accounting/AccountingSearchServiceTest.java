package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.accounting.AccountingExportData;
import fi.hel.verkkokauppa.order.model.refund.Refund;
import fi.hel.verkkokauppa.order.test.utils.SearchAfterServiceTestUtils;
import fi.hel.verkkokauppa.order.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@RunIfProfile(profile = "local")
@TestPropertySource(properties = {"elasticsearch.search-after-page-size = 7"})
@Slf4j
public class AccountingSearchServiceTest extends SearchAfterServiceTestUtils {

    @Autowired
    AccountingSearchService searchServiceToTest;

    @Autowired
    AccountingExportDataService accountingExportDataService;

    @Value("${elasticsearch.search-after-page-size}")
    private int elasticsearchSearchAfterPageSize;

    @After
    public void tearDown() {
        try {
            deleteNotAccountedOrders();
            deleteNotAccountedRefunds();
            clearAccountingExportData();
        } catch (Exception e) {
            log.info("delete error {}", e.toString());
        }
    }

    @Test
    public void testGetNotExportedAccountingExportData() throws Exception {
        log.info("running testGetNotExportedAccountingExportData");
        createAccountingExportData(10);
        List<AccountingExportData> resultList;
        long expectedTotalHits = notExportedAccountingExportDataCount();

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);
        resultList = searchServiceToTest.getNotExportedAccountingExportData();
        log.info("Result list size: " + resultList.size());

        assertFalse("Result list should not be empty", resultList.isEmpty());

        assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", expectedTotalHits, resultList.size());
    }

    @Test
    public void testFindNotAccountedOrders() throws Exception {
        log.info("running testFindNotAccountedOrders");
        createOrder(10);
        List<Order> resultList;
        long expectedTotalHits = notAccountedOrderCount();

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);
        resultList = searchServiceToTest.findNotAccountedOrders();
        log.info("Result list size: " + resultList.size());

        assertFalse("Result list should not be empty", resultList.isEmpty());

        assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", expectedTotalHits, resultList.size());
    }

    @Test
    public void testFindNotAccountedRefunds() throws Exception {
        log.info("running testFindNotAccountedRefunds");
        createRefund(10);
        List<Refund> resultList;
        long expectedTotalHits = notAccountedRefundCount();

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);
        resultList = searchServiceToTest.findNotAccountedRefunds();
        log.info("Result list size: " + resultList.size());

        assertFalse("Result list should not be empty", resultList.isEmpty());

        assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", expectedTotalHits, resultList.size());
    }
}

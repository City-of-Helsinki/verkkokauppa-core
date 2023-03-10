package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.accounting.AccountingExportData;
import fi.hel.verkkokauppa.order.test.utils.SearchAfterServiceTestUtils;
import fi.hel.verkkokauppa.order.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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

    @Test
    public void whenGetNotExportedAccountingExportDataAndNoHits_thenSuccess() throws Exception {
        log.info("running whenGetNotExportedAccountingExportDataAndNoHits_thenSuccess");
        clearAccountingExportData();
        List<AccountingExportData> resultList;

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);
        resultList = searchServiceToTest.getNotExportedAccountingExportData();
        log.info("Result list size: " + resultList.size());

        assertTrue("Result list be empty", resultList.isEmpty());
    }

    @Test
    public void whenGetNotExportedAccountingExportData_thenSuccess() throws Exception {
        log.info("running whenGetNotExportedAccountingExportData_thenSuccess");
        createAccountingExportData(10);
        List<AccountingExportData> resultList;
        long expectedTotalHits = accountingExportDataCount();

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);
        resultList = searchServiceToTest.getNotExportedAccountingExportData();
        log.info("Result list size: " + resultList.size());

        assertFalse("Result list should not be empty", resultList.isEmpty());

        assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", expectedTotalHits, resultList.size());
    }

    @Test
    public void whenFindNotAccountedNoHits_thenSuccess() throws Exception {
        log.info("running whenFindNotAccountedNoHits_thenSuccess");
        deleteNotAccountedOrders();
        List<Order> resultList;

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);
        resultList = searchServiceToTest.findNotAccounted();
        log.info("Result list size: " + resultList.size());

        assertTrue("Result list should be empty", resultList.isEmpty());
    }

    @Test
    public void whenFindNotAccounted_thenSuccess() throws Exception {
        log.info("running whenFindNotAccounted_thenSuccess");
        createOrder(10);
        List<Order> resultList;
        long expectedTotalHits = notAccountedOrderCount();

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);
        resultList = searchServiceToTest.findNotAccounted();
        log.info("Result list size: " + resultList.size());

        assertFalse("Result list should not be empty", resultList.isEmpty());

        assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", expectedTotalHits, resultList.size());
    }
}

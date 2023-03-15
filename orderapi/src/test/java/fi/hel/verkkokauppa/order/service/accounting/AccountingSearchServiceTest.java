package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.accounting.AccountingExportData;
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

    private ArrayList<String> toBeDeletedOrderById = new ArrayList<>();
    private ArrayList<String> toBeDeletedAccountingById = new ArrayList<>();

    @After
    public void tearDown() {
        try {
            deleteNotAccountedOrders(toBeDeletedOrderById);
            clearAccountingExportData(toBeDeletedAccountingById);
            // Clear list because all deleted
            toBeDeletedOrderById = new ArrayList<>();
            toBeDeletedAccountingById = new ArrayList<>();
        } catch (Exception e) {
            log.info("delete error {}", e.toString());
            toBeDeletedOrderById = new ArrayList<>();
            toBeDeletedAccountingById = new ArrayList<>();
        }
    }

    @Test
    public void whenGetNotExportedAccountingExportData_thenSuccess() throws Exception {
        log.info("running whenGetNotExportedAccountingExportData_thenSuccess");
        createAccountingExportData(10, toBeDeletedAccountingById);
        List<AccountingExportData> resultList;
        long expectedTotalHits = accountingExportDataCount();

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);
        resultList = searchServiceToTest.getNotExportedAccountingExportData();
        log.info("Result list size: " + resultList.size());

        assertFalse("Result list should not be empty", resultList.isEmpty());

        assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", expectedTotalHits, resultList.size());
    }

    @Test
    public void whenFindNotAccounted_thenSuccess() throws Exception {
        log.info("running whenFindNotAccounted_thenSuccess");
        createOrder(10, toBeDeletedOrderById);
        List<Order> resultList;
        long expectedTotalHits = notAccountedOrderCount();

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);
        resultList = searchServiceToTest.findNotAccounted();
        log.info("Result list size: " + resultList.size());

        assertFalse("Result list should not be empty", resultList.isEmpty());

        assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", expectedTotalHits, resultList.size());
    }
}

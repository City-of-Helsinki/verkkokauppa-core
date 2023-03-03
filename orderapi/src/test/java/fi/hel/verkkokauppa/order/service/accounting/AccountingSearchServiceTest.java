package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.accounting.AccountingExportData;
import fi.hel.verkkokauppa.order.test.utils.SearchAfterServiceTestUtils;
import fi.hel.verkkokauppa.order.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


@RunWith(SpringJUnit4ClassRunner.class )
@SpringBootTest
@RunIfProfile(profile = "local")
@TestPropertySource(properties = { "elasticsearch.search-after-page-size = 7" })
@ComponentScan("fi.hel.verkkokauppa.order.service.elasticsearch")
@Slf4j
public class AccountingSearchServiceTest extends SearchAfterServiceTestUtils {

    @Autowired
    private Environment env;

    @Autowired
    AccountingSearchService searchServiceToTest;

    @Autowired
    AccountingExportDataService accountingExportDataService;

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


}
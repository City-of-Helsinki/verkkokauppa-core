package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.order.service.elasticsearch.SearchAfterService;
import fi.hel.verkkokauppa.order.test.utils.SearchAfterServiceTestUtils;
import fi.hel.verkkokauppa.order.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import java.util.List;

import static org.junit.Assert.*;


@RunWith(SpringJUnit4ClassRunner.class )
@SpringBootTest
@RunIfProfile(profile = "local")
@TestPropertySource(properties = { "elasticsearch.search-after-page-size = 8" })
@ComponentScan("fi.hel.verkkokauppa.order.service.elasticsearch")
@Slf4j
public class SearchAfterServiceTest extends SearchAfterServiceTestUtils {

    @Autowired
    private SearchAfterService searchAfterService;

    @Autowired
    private Environment env;

    @Autowired
    AccountingExportDataService accountingExportDataService;

    @Before
    public void initTests(){
        // creates test data to Elasticsearch if none exists
        createAccountingExportData(10);
        createOrder(10);
    }

    @Test
    public void whenNoSort_thenSuccess()  {
        log.info("running whenNoSort_thenSuccess");
        int pageSize = Integer.parseInt(env.getProperty("elasticsearch.search-after-page-size"));

        try{
            log.info("elasticsearch.search-after-page-size: " + pageSize);
            BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("exported"));

            NativeSearchQuery query = new NativeSearchQueryBuilder()
                    .withQuery(qb).build();

            SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                    query,
                    null,
                    "accountingexportdatas");
            log.info(searchRequest.toString());

            List<SortBuilder<?>> sorts = searchRequest.source().sorts();
            assertTrue("sorts should be empty", (sorts==null || sorts.size()==0));

            org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

            log.info("Result list size: " + hits.length);

            assertFalse("Result list should not be empty", (hits.length==0));

            assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", pageSize, hits.length);

        }
        catch (Exception e){
            assertFalse("Exception: " + e.getMessage(),true);
        }
    }

    @Test
    public void whenSort_thenSuccess() {
        log.info("running whenSort_thenSuccess");
        long expectedTotalHits = accountingExportDataCount();

        try{
            log.info("elasticsearch.search-after-page-size: " + env.getProperty("elasticsearch.search-after-page-size"));
            BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("exported"));

            NativeSearchQuery query = new NativeSearchQueryBuilder()
                    .withQuery(qb).build();

            SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                    query,
                    new SortBuilder[] {new FieldSortBuilder("_id").order(SortOrder.DESC)},
                    "accountingexportdatas");
            log.info(searchRequest.toString());

            List<SortBuilder<?>> sorts = searchRequest.source().sorts();
            assertFalse("sorts should not be empty", (sorts==null || sorts.size()==0));

            org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

            log.info("Result list size: " + hits.length);

            assertFalse("Result list should not be empty", (hits.length==0));

            // maximum number of total hits we can receive is 10 000. Cannot verify counts with 10000 or more
            if(expectedTotalHits < 10000) {
                assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", expectedTotalHits, hits.length);
            }

        }
        catch (Exception e){
            assertFalse("Exception: " + e.getMessage(),true);
        }
    }

    @Test
    public void whenNoQuery_thenSuccess() {
        log.info("running whenNoQuery_thenSuccess");
        long expectedTotalHits = accountingExportDataCount();

        try{
            log.info("elasticsearch.search-after-page-size: " + env.getProperty("elasticsearch.search-after-page-size"));

            SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                    null,
                    new SortBuilder[] {new FieldSortBuilder("_id").order(SortOrder.DESC)},
                    "accountingexportdatas");
            log.info(searchRequest.toString());

            List<SortBuilder<?>> sorts = searchRequest.source().sorts();
            assertFalse("sorts should not be empty", (sorts==null || sorts.size()==0));

            org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

            log.info("Result list size: " + hits.length);

            assertFalse("Result list should not be empty", (hits.length==0));

            // maximum number of total hits we can receive is 10 000. Cannot verify counts with 10000 or more
            if(expectedTotalHits < 10000) {
                assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", expectedTotalHits, hits.length);
            }

        }
        catch (Exception e){
            assertFalse("Exception: " + e.getMessage(),true);
        }
    }

    @Test
    public void whenNoIndice_thenFail() {
        log.info("running whenNoQuery_thenSuccess");
        long expectedTotalHits = accountingExportDataCount();

        try{
            log.info("elasticsearch.search-after-page-size: " + env.getProperty("elasticsearch.search-after-page-size"));

            try {
                SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                        null,
                        new SortBuilder[]{new FieldSortBuilder("_id").order(SortOrder.DESC)},
                        null);
                log.info(searchRequest.toString());

                assertFalse("Empty indices should throw exception", true);
            }catch (Exception e){
                // should receive exception when no indices
                log.info("Pass : " + e.getMessage());
            }

            try {
                org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(new SearchRequest());
                assertFalse("Empty indices should throw exception", true);
            } catch (Exception e){
                // should receive exception when no indices
                log.info("Pass : " + e.getMessage());
            }

        }
        catch (Exception e){
            assertFalse("Exception: " + e.getMessage(),true);
        }
    }

    @Test
    public void whenSortAndMultipleIndices_thenSuccess() {
        log.info("running whenSort_thenSuccess");
        long expectedTotalHits = accountingExportDataCount();

        try{
            log.info("elasticsearch.search-after-page-size: " + env.getProperty("elasticsearch.search-after-page-size"));
            BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("exported"));

            NativeSearchQuery query = new NativeSearchQueryBuilder()
                    .withQuery(qb).build();

            SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                    query,
                    new SortBuilder[] {new FieldSortBuilder("_id").order(SortOrder.DESC)},
                    "accountingexportdatas","orders");
            log.info(searchRequest.toString());

            List<SortBuilder<?>> sorts = searchRequest.source().sorts();
            assertFalse("sorts should not be empty", (sorts==null || sorts.size()==0));

            org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

            log.info("Result list size: " + hits.length);

            assertFalse("Result list should not be empty", (hits.length==0));

            // maximum number of total hits we can receive is 10 000. Cannot verify counts with 10000 or more
            if(expectedTotalHits < 10000) {
                assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", expectedTotalHits, hits.length);
            }

        }
        catch (Exception e){
            assertFalse("Exception: " + e.getMessage(),true);
        }
    }

    @Test
    public void whenNoSortAndMultipleIndices_thenSuccess() {
        log.info("running whenNoSort_thenSuccess");
        int pageSize = Integer.parseInt(env.getProperty("elasticsearch.search-after-page-size"));

        try{
            log.info("elasticsearch.search-after-page-size: " + pageSize);
            BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("exported"));

            NativeSearchQuery query = new NativeSearchQueryBuilder()
                    .withQuery(qb).build();

            SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                    query,
                    null,
                    "accountingexportdatas","orders");
            log.info(searchRequest.toString());

            List<SortBuilder<?>> sorts = searchRequest.source().sorts();
            assertTrue("sorts should be empty", (sorts==null || sorts.size()==0));

            org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

            log.info("Result list size: " + hits.length);

            assertFalse("Result list should not be empty", (hits.length==0));

            assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", pageSize, hits.length);

        }
        catch (Exception e){
            assertFalse("Exception: " + e.getMessage(),true);
        }
    }

}

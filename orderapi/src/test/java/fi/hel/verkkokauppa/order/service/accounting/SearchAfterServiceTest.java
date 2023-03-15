package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.order.model.Order;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@RunIfProfile(profile = "local")
@TestPropertySource(properties = {"elasticsearch.search-after-page-size = 8"})
@ComponentScan("fi.hel.verkkokauppa.order.service.elasticsearch")
@Slf4j
public class SearchAfterServiceTest extends SearchAfterServiceTestUtils {

    @Autowired
    private SearchAfterService searchAfterService;

    @Autowired
    AccountingExportDataService accountingExportDataService;

    @Value("${elasticsearch.search-after-page-size}")
    private int elasticsearchSearchAfterPageSize;


    private ArrayList<String> toBeDeletedOrderById = new ArrayList<>();
    private ArrayList<String> toBeDeletedAccountingById = new ArrayList<>();

    @Before
    public void initTests() {
        // creates test data to Elasticsearch if none exists
        createAccountingExportData(10, toBeDeletedAccountingById);
        createOrder(10, toBeDeletedOrderById);
    }

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
    public void whenNoSort_thenSuccess() throws Exception {
        log.info("running whenNoSort_thenSuccess");

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("exported"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb).build();

        SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                query,
                null,
                "accountingexportdatas");
        log.info(searchRequest.toString());

        List<SortBuilder<?>> sorts = searchRequest.source().sorts();
        assertTrue("sorts should be empty", (sorts == null || sorts.size() == 0));

        org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

        log.info("Result list size: " + hits.length);

        assertNotEquals("Result list should not be empty", 0, hits.length);

        assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", elasticsearchSearchAfterPageSize, hits.length);
    }

    @Test
    public void whenSort_thenSuccess() throws Exception {
        log.info("running whenSort_thenSuccess");
        long expectedTotalHits = accountingExportDataCount();

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("exported"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb).build();

        SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                query,
                new SortBuilder[]{new FieldSortBuilder("_id").order(SortOrder.DESC)},
                "accountingexportdatas");
        log.info(searchRequest.toString());

        List<SortBuilder<?>> sorts = searchRequest.source().sorts();
        assertNotNull("sorts should not be empty", sorts);
        assertNotEquals("sorts should not be empty", 0, sorts.size());

        org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

        log.info("Result list size: " + hits.length);

        assertNotEquals("Result list should not be empty", 0, hits.length);

        assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", expectedTotalHits, hits.length);
    }

    @Test
    public void whenNoQuery_thenSuccess() throws Exception {
        log.info("running whenNoQuery_thenSuccess");
        long expectedTotalHits = accountingExportDataCount();

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);

        SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                null,
                new SortBuilder[]{new FieldSortBuilder("_id").order(SortOrder.DESC)},
                "accountingexportdatas");
        log.info(searchRequest.toString());

        List<SortBuilder<?>> sorts = searchRequest.source().sorts();
        assertFalse("sorts should not be empty", (sorts == null || sorts.size() == 0));

        org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

        log.info("Result list size: " + hits.length);

        assertNotEquals("Result list should not be empty", 0, hits.length);

        assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", expectedTotalHits, hits.length);
    }

    @Test
    public void whenNoIndice_thenFail() {
        log.info("running whenNoIndice_thenFail");

        Exception exception = assertThrows(Exception.class, () -> {
            SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                    null,
                    searchAfterService.buildSortWithId(),
                    null);
        });

        String expectedMessage = "at least one indice is needed";
        String actualMessage = exception.getMessage();

        assertTrue("Not expected Exception. Exception message: " + exception.getMessage(), actualMessage.contains(expectedMessage));

        exception = assertThrows(Exception.class, () -> {
            org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(
                    new SearchRequest()
            );
        });

        expectedMessage = "SearchRequest should have at least one indice";
        actualMessage = exception.getMessage();

        assertTrue("Not expected Exception. Exception message: " + exception.getMessage(), actualMessage.contains(expectedMessage));
    }

    @Test
    public void whenSortAndMultipleIndices_thenSuccess() throws Exception {
        log.info("running whenSort_thenSuccess");
        long expectedTotalHits = accountingExportDataCount() + notAccountedOrderCount();

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("exported"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb).build();

        SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                query,
                new SortBuilder[]{new FieldSortBuilder("_id").order(SortOrder.DESC)},
                "accountingexportdatas", "orders");
        log.info(searchRequest.toString());

        List<SortBuilder<?>> sorts = searchRequest.source().sorts();
        assertFalse("sorts should not be empty", (sorts == null || sorts.size() == 0));

        org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

        log.info("Result list size: " + hits.length);

        assertNotEquals("Result list should not be empty", 0, hits.length);

        assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", expectedTotalHits, hits.length);
    }

    @Test
    public void whenNoSortAndMultipleIndices_thenSuccess() throws Exception {
        log.info("running whenNoSort_thenSuccess");

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("exported"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb).build();

        SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                query,
                null,
                "accountingexportdatas", "orders");
        log.info(searchRequest.toString());

        List<SortBuilder<?>> sorts = searchRequest.source().sorts();
        assertTrue("sorts should be empty", (sorts == null || sorts.size() == 0));

        org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

        log.info("Result list size: " + hits.length);

        assertNotEquals("Result list should not be empty", 0, hits.length);

        assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", elasticsearchSearchAfterPageSize, hits.length);
    }

    @Test
    public void whenNoHits_thenSuccess() throws Exception {
        log.info("running whenNoSort_thenSuccess");

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);
        // query something impossible
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("timestamp"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb).build();

        SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                query,
                null,
                "accountingexportdatas"
        );
        log.info(searchRequest.toString());

        List<SortBuilder<?>> sorts = searchRequest.source().sorts();
        assertNull("sorts should be empty", sorts);

        org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

        log.info("Result list size: " + hits.length);

        assertEquals("Result list should be empty", 0, hits.length);
    }

    @Test
    public void whenBuildListFromHits_thenSuccess() throws Exception {
        log.info("running whenBuildListFromHitsWithMultipleIndices_thenSuccess");
        long expectedTotalHits = notAccountedOrderCount();

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("exported"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb).build();

        SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                query,
                new SortBuilder[]{new FieldSortBuilder("_id").order(SortOrder.DESC)},
                "orders");
        log.info(searchRequest.toString());
        org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);
        log.info("Result list size: " + hits.length);

        final List<Order> exportData = searchAfterService.buildListFromHits(hits, Order.class);

        assertNotEquals("Result list should not be empty", 0, exportData.size());
        assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", expectedTotalHits, exportData.size());
    }

}

package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.accounting.AccountingExportData;
import fi.hel.verkkokauppa.order.service.elasticsearch.SearchAfterService;
import fi.hel.verkkokauppa.order.test.utils.SearchAfterServiceTestUtils;
import fi.hel.verkkokauppa.order.testing.annotations.RunIfProfile;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
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

    @Before
    public void initTests() {
        // creates test data to Elasticsearch if none exists
        createAccountingExportData(10);
        createOrder(10);
    }

    @After
    public void tearDown() {
        try {
            deleteNotAccountedOrders();
            clearAccountingExportData();
        } catch (Exception e) {
            log.info("delete error {}", e.toString());
        }
    }

    @Test
    public void testSearchWithoutSort() throws Exception {
        log.info("running testSearchWithoutSort");

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("exported"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb).build();

        SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                null,
                null,
                AccountingExportData.INDEX_NAME);
        log.info(searchRequest.toString());

        List<SortBuilder<?>> sorts = searchRequest.source().sorts();
        assertNull("sorts should be empty", sorts);

        SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

        log.info("Result list size: " + hits.length);

        assertNotEquals("Result list should not be empty", 0, hits.length);

        assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", elasticsearchSearchAfterPageSize, hits.length);
    }

    @Test
    public void testSearchWithSort() throws Exception {
        log.info("running testSearchWithSort");
        long expectedTotalHits = notExportedAccountingExportDataCount();

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("exported"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb).build();

        SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                query,
                new SortBuilder[]{new FieldSortBuilder("_id").order(SortOrder.DESC)},
                AccountingExportData.INDEX_NAME);
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
    public void testSearchWithoutQuery() throws Exception {
        log.info("running testSearchWithoutQuery");
        long expectedTotalHits = accountingExportDataCount();

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);

        SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                null,
                new SortBuilder[]{new FieldSortBuilder("_id").order(SortOrder.DESC)},
                AccountingExportData.INDEX_NAME);
        log.info(searchRequest.toString());

        List<SortBuilder<?>> sorts = searchRequest.source().sorts();
        assertFalse("sorts should not be empty", (sorts == null || sorts.size() == 0));

        org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

        log.info("Result list size: " + hits.length);

        assertNotEquals("Result list should not be empty", 0, hits.length);

        assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", expectedTotalHits, hits.length);
    }

    @Test
    public void testSearchWithoutIndices() {
        log.info("running testSearchWithoutIndices");

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
    public void testSearchWithSortAndMultipleIndices() throws Exception {
        log.info("running testSearchWithSortAndMultipleIndices");
        long expectedTotalHits = notExportedAccountingExportDataCount() + orderCount();

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("exported"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb).build();

        SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                query,
                new SortBuilder[]{new FieldSortBuilder("_id").order(SortOrder.DESC)},
                AccountingExportData.INDEX_NAME, Order.INDEX_NAME);
        log.info(searchRequest.toString());

        List<SortBuilder<?>> sorts = searchRequest.source().sorts();
        assertFalse("sorts should not be empty", (sorts == null || sorts.size() == 0));

        org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

        log.info("Result list size: " + hits.length);

        assertNotEquals("Result list should not be empty", 0, hits.length);

        assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", expectedTotalHits, hits.length);
    }

    @Test
    public void testSearchWithNoSortAndMultipleIndices() throws Exception {
        log.info("running testSearchWithNoSortAndMultipleIndices");

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("exported"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb).build();

        SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                query,
                null,
                AccountingExportData.INDEX_NAME, Order.INDEX_NAME);
        log.info(searchRequest.toString());

        List<SortBuilder<?>> sorts = searchRequest.source().sorts();
        assertTrue("sorts should be empty", (sorts == null || sorts.size() == 0));

        SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

        log.info("Result list size: " + hits.length);

        assertNotEquals("Result list should not be empty", 0, hits.length);

        assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", elasticsearchSearchAfterPageSize, hits.length);
    }

    @Test
    public void testSearchWithNoHits() throws Exception {
        log.info("running testSearchWithNoHits");

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);
        // query something impossible
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("timestamp"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb).build();

        SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                query,
                null,
                AccountingExportData.INDEX_NAME
        );
        log.info(searchRequest.toString());

        List<SortBuilder<?>> sorts = searchRequest.source().sorts();
        assertNull("sorts should be empty", sorts);

        SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

        log.info("Result list size: " + hits.length);

        assertEquals("Result list should be empty", 0, hits.length);
    }

    @Test
    public void testBuildListFromHits() throws Exception {
        log.info("running testBuildListFromHits");
        long expectedTotalHits = notExportedAccountingExportDataCount();

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("exported"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb).build();

        SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                query,
                new SortBuilder[]{new FieldSortBuilder("_id").order(SortOrder.DESC)},
                AccountingExportData.INDEX_NAME);
        log.info(searchRequest.toString());
        SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);
        log.info("Result list size: " + hits.length);

        final List<Order> exportData = searchAfterService.buildListFromHits(hits, Order.class);

        assertNotEquals("Result list should not be empty", 0, exportData.size());
        assertEquals("Number of results is not the same as number of accountingExportData in ElasticSearch", expectedTotalHits, exportData.size());
    }

}

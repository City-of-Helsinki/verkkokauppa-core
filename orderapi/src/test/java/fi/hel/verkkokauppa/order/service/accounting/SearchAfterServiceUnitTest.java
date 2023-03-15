package fi.hel.verkkokauppa.order.service.accounting;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.accounting.AccountingExportData;
import fi.hel.verkkokauppa.order.service.elasticsearch.SearchAfterService;
import fi.hel.verkkokauppa.order.testing.annotations.UnitTest;
import fi.hel.verkkokauppa.order.testing.utils.AutoMockBeanFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchResponseSections;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.DocValueFormat;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;


@RunWith(SpringJUnit4ClassRunner.class)
@UnitTest
@WebMvcTest(SearchAfterService.class)
@ContextConfiguration(classes = AutoMockBeanFactory.class)
@TestPropertySource(properties = {"elasticsearch.search-after-page-size = 10000"})
@Slf4j
public class SearchAfterServiceUnitTest {

    @MockBean
    private RestHighLevelClient highLevelClientMock;

    @MockBean
    private SearchAfterService searchAfterService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${elasticsearch.search-after-page-size}")
    private int elasticsearchSearchAfterPageSize;

    @Before
    public void setupTests() throws Exception {
        ReflectionTestUtils.setField(searchAfterService, "highLevelClient", highLevelClientMock);
        ReflectionTestUtils.setField(searchAfterService, "elasticsearchSearchAfterPageSize", elasticsearchSearchAfterPageSize);
        ReflectionTestUtils.setField(searchAfterService, "objectMapper", objectMapper);

        when(searchAfterService.buildSearchAfterSearchRequest(any(), any(), any())).thenCallRealMethod();
        when(searchAfterService.executeSearchRequest(any())).thenCallRealMethod();
        when(searchAfterService.buildSortWithId()).thenCallRealMethod();
    }

    @Test
    public void testSearchWithoutSort() throws Exception {
        log.info("running testSearchWithoutSort");
        SearchRequest searchRequest;

        SearchResponse mockedResponse = getTestSearchResponse(2);
        when(searchAfterService.search(any(SearchRequest.class), any(RequestOptions.class))).thenReturn(mockedResponse);

        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("exported"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb).build();

        searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                query,
                null,
                AccountingExportData.INDEX_NAME);
        log.info(searchRequest.toString());

        List<SortBuilder<?>> sorts = searchRequest.source().sorts();
        assertNull("sorts should be empty", sorts);

        org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

        log.info("Result list size: " + hits.length);

        assertNotEquals("Result list should not be empty", 0, hits.length);
    }

    @Test
    public void testSearchWithSort() throws Exception {
        log.info("running testSearchWithSort");

        SearchResponse mockedResponse = getTestSearchResponse(2);
        when(searchAfterService.search(any(SearchRequest.class), any(RequestOptions.class))).thenReturn(mockedResponse);

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("exported"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb).build();

        SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                query,
                searchAfterService.buildSortWithId(),
                AccountingExportData.INDEX_NAME);
        log.info(searchRequest.toString());

        List<SortBuilder<?>> sorts = searchRequest.source().sorts();
        assertNotNull("sorts should not be empty", sorts);
        assertNotEquals("sorts should not be empty", 0, sorts.size());

        org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

        assertNotEquals("Result list should not be empty", 0, hits.length);
    }

    @Test
    public void testSearchWithoutQuery() throws Exception {
        log.info("running testSearchWithoutQuery");

        SearchResponse mockedResponse = getTestSearchResponse(2);
        when(searchAfterService.search(any(SearchRequest.class), any(RequestOptions.class))).thenReturn(mockedResponse);

        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);

        SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                null,
                searchAfterService.buildSortWithId(),
                AccountingExportData.INDEX_NAME);
        log.info(searchRequest.toString());

        List<SortBuilder<?>> sorts = searchRequest.source().sorts();
        assertNotNull("sorts should not be empty", sorts);
        assertNotEquals("sorts should not be empty", 0, sorts.size());

        org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

        log.info("Result list size: " + hits.length);

        assertNotEquals("Result list should not be empty", 0, hits.length);
    }

    @Test
    public void testSearchWithoutIndices() throws Exception {
        log.info("running testSearchWithoutIndices");

        when(searchAfterService.search(any(), any())).thenCallRealMethod();

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
    public void testSearchWithMultiplePages() throws Exception {
        log.info("running testSearchWithMultiplePages");

        int expectedTotalHits = 22500; // total amount to compare to
        SearchResponse mockedResponse = getTestSearchResponse(10000);
        SearchResponse mockedResponse2 = getTestSearchResponse(10000);
        SearchResponse mockedResponse3 = getTestSearchResponse(2500);

        when(searchAfterService.search(any(SearchRequest.class), any(RequestOptions.class)))
                .thenReturn(mockedResponse)
                .thenReturn(mockedResponse2)
                .thenReturn(mockedResponse3);
        log.info("elasticsearch.search-after-page-size: " + elasticsearchSearchAfterPageSize);
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("exported"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb).build();

        SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                query,
                searchAfterService.buildSortWithId(),
                AccountingExportData.INDEX_NAME, Order.INDEX_NAME
        );
        log.info(searchRequest.toString());

        List<SortBuilder<?>> sorts = searchRequest.source().sorts();
        assertNotNull("sorts should not be empty", sorts);
        assertNotEquals("sorts should not be empty", 0, sorts.size());

        org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

        assertEquals("Number of results is not the same as number of hits there should be", expectedTotalHits, hits.length);
    }

    @Test
    public void testSearchWithoutHits() throws Exception {
        log.info("running testSearchWithoutHits");

        SearchResponse mockedResponse = getTestSearchResponse(0);
        when(searchAfterService.search(any(SearchRequest.class), any(RequestOptions.class))).thenReturn(mockedResponse);

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

        org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

        log.info("Result list size: " + hits.length);

        assertEquals("Result list should be empty", 0, hits.length);
    }

    @Test
    public void testBuildListFromHits() throws IOException {
        log.info("running testBuildListFromHits");
        int testHitCount = 5;

        SearchResponse mockedResponse = getTestSearchResponse(testHitCount);
        when(searchAfterService.buildListFromHits(any(), any())).thenCallRealMethod();

        final List<AccountingExportData> exportData = searchAfterService.buildListFromHits(mockedResponse.getHits().getHits(), AccountingExportData.class);

        assertNotEquals("Result list should not be empty", 0, exportData.size());
        assertEquals("Wrong amount of records in export data", testHitCount, exportData.size());
    }


    // test utility method for creating SearchResponse
    private SearchResponse getTestSearchResponse(int hitCount) {

        // create array of hits
        SearchHit[] testHits = new SearchHit[hitCount];
        for (int i = 0; i < hitCount; i++) {

            // create dummy source for parsing
            BytesReference source = new BytesArray("{\n" +
                    "    \"_class\" : \"fi.hel.verkkokauppa.order.model.accounting.AccountingExportData\",\n" +
                    "    \"accountingSlipId\" : \"ae192394-df3f-42ed-8d9e-8834c16cce4e-testaccounting\",\n" +
                    "    \"timestamp\" : \"2023-03-09\",\n" +
                    "    \"xml\" : \"Some text 9\"\n" +
                    "  }");

            Map<String, Object> data = new HashMap<>();

            data.put("_index", "accountingexportdatas");
            data.put("_type", new Text("_doc"));
            data.put("_id", Integer.toString(i));
            data.put("_source", source);

            testHits[i] = SearchHit.createFromMap(data);

            //  put in some sort values so new page can be searched for
            testHits[i].sortValues(
                    new Object[]{"111"},
                    new DocValueFormat[]{DocValueFormat.RAW}
            );
        }

        // create number of totalHits
        int totalHits = (hitCount <= 10000 ? hitCount : 10000);

        SearchResponse response = new SearchResponse(
                new SearchResponseSections(
                        new SearchHits(
                                testHits,
                                new TotalHits(totalHits, TotalHits.Relation.EQUAL_TO),
                                1),
                        null,
                        null,
                        false,
                        false,
                        null,
                        1),
                null,
                0,
                0,
                0,
                0,
                null,
                null);

        return response;
    }


}

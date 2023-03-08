package fi.hel.verkkokauppa.order.service.accounting;

import fi.hel.verkkokauppa.order.service.elasticsearch.SearchAfterService;
import fi.hel.verkkokauppa.order.testing.annotations.UnitTest;
import fi.hel.verkkokauppa.order.testing.utils.AutoMockBeanFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchResponseSections;
import org.elasticsearch.client.*;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.DocValueFormat;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


@RunWith(SpringJUnit4ClassRunner.class )
@UnitTest
@WebMvcTest(SearchAfterService.class)
@ContextConfiguration(classes = AutoMockBeanFactory.class)
@Slf4j
public class SearchAfterServiceUnitTest{

    @MockBean
    private RestHighLevelClient highLevelClientMock;

    @MockBean
    private SearchAfterService searchAfterService;

    @Mock
    private Environment env;




    @Test
    public void whenNoSort_thenSuccess()  {
        log.info("running whenNoSort_thenSuccess");
        SearchRequest searchRequest;
        try{
            SearchResponse mockedResponse = getTestSearchResponse(2);
            doReturn("3").when(env).getProperty("elasticsearch.search-after-page-size");
            ReflectionTestUtils.setField(searchAfterService, "env", env);
            ReflectionTestUtils.setField(searchAfterService, "highLevelClient", highLevelClientMock);
            when(searchAfterService.buildSearchAfterSearchRequest(any(),any(),any())).thenCallRealMethod();
            when(searchAfterService.executeSearchRequest(any())).thenCallRealMethod();
            when(searchAfterService.search(any(SearchRequest.class), any(RequestOptions.class))).thenReturn(mockedResponse);

            BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("exported"));

            NativeSearchQuery query = new NativeSearchQueryBuilder()
                    .withQuery(qb).build();

            searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                    query,
                    null,
                    "accountingexportdatas");
            log.info(searchRequest.toString());

            List<SortBuilder<?>> sorts = searchRequest.source().sorts();
            assertTrue("sorts should be empty", (sorts==null || sorts.size()==0));

            org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

            log.info("Result list size: " + hits.length);

            assertFalse("Result list should not be empty", (hits.length==0));


        }
        catch (Exception e){
            assertFalse("Exception: " + e.getMessage(),true);
        }
    }

    @Test
    public void whenSort_thenSuccess() {
        log.info("running whenSort_thenSuccess");


        try{
            SearchResponse mockedResponse = getTestSearchResponse(2);
            doReturn("3").when(env).getProperty("elasticsearch.search-after-page-size");
            ReflectionTestUtils.setField(searchAfterService, "env", env);
            ReflectionTestUtils.setField(searchAfterService, "highLevelClient", highLevelClientMock);
            when(searchAfterService.buildSearchAfterSearchRequest(any(),any(),any())).thenCallRealMethod();
            when(searchAfterService.executeSearchRequest(any())).thenCallRealMethod();
            when(searchAfterService.search(any(SearchRequest.class), any(RequestOptions.class))).thenReturn(mockedResponse);

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



        }
        catch (Exception e){
            assertFalse("Exception: " + e.getMessage(),true);
        }
    }

    @Test
    public void whenNoQuery_thenSuccess() {
        log.info("running whenNoQuery_thenSuccess");

        try{
            SearchResponse mockedResponse = getTestSearchResponse(2);
            doReturn("3").when(env).getProperty("elasticsearch.search-after-page-size");
            ReflectionTestUtils.setField(searchAfterService, "env", env);
            ReflectionTestUtils.setField(searchAfterService, "highLevelClient", highLevelClientMock);
            when(searchAfterService.buildSearchAfterSearchRequest(any(),any(),any())).thenCallRealMethod();
            when(searchAfterService.executeSearchRequest(any())).thenCallRealMethod();
            when(searchAfterService.search(any(SearchRequest.class), any(RequestOptions.class))).thenReturn(mockedResponse);

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



        }
        catch (Exception e){
            assertFalse("Exception: " + e.getMessage(),true);
        }
    }

    @Test
    public void whenNoIndice_thenFail() {
        log.info("running whenNoQuery_thenSuccess");
        try{
            when(searchAfterService.buildSearchAfterSearchRequest(any(),any(),any())).thenCallRealMethod();
            when(searchAfterService.executeSearchRequest(any())).thenCallRealMethod();
            when(searchAfterService.search(any(),any())).thenCallRealMethod();

            try {
                SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                        null,
                        new SortBuilder[]{new FieldSortBuilder("_id").order(SortOrder.DESC)},
                        null);
                log.info(searchRequest.toString());

                assertFalse("Empty indices should throw exception", true);
            }catch (Exception e){
                // should receive exception when no indices
                log.info("Pass. Exception received : " + e.getMessage());
            }

            try {
                org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(new SearchRequest());
                assertFalse("Empty indices should throw exception", true);
            } catch (Exception e){
                // should receive exception when no indices
                log.info("Pass. Exception received : " + e.getMessage());
            }

        }
        catch (Exception e){
            assertFalse("Exception: " + e.getMessage(),true);
        }
    }

    @Test
    public void whenMoreThanMaxPageHits_thenSuccess() {
        log.info("running whenMoreThanMaxPageHits_thenSuccess");

        try{
            int expectedTotalHits = 22500; // total amount to compare to
            SearchResponse mockedResponse = getTestSearchResponse(10000);
            SearchResponse mockedResponse2 = getTestSearchResponse(10000);
            SearchResponse mockedResponse3 = getTestSearchResponse(2500);
            doReturn("10000").when(env).getProperty("elasticsearch.search-after-page-size");
            ReflectionTestUtils.setField(searchAfterService, "env", env);
            ReflectionTestUtils.setField(searchAfterService, "highLevelClient", highLevelClientMock);
            when(searchAfterService.buildSearchAfterSearchRequest(any(),any(),any())).thenCallRealMethod();
            when(searchAfterService.executeSearchRequest(any())).thenCallRealMethod();
            when(searchAfterService.search(any(SearchRequest.class), any(RequestOptions.class)))
                    .thenReturn(mockedResponse)
                    .thenReturn(mockedResponse2)
                    .thenReturn(mockedResponse3);
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


            assertEquals("Number of results is not the same as number of hits there should be", expectedTotalHits, hits.length);

        }
        catch (Exception e){
            assertFalse("Exception: " + e.getMessage(),true);
        }
    }

    @Test
    public void whenNoHits_thenSuccess() {
        log.info("running whenNoSort_thenSuccess");

        try{
            SearchResponse mockedResponse = getTestSearchResponse(0);
            doReturn("3").when(env).getProperty("elasticsearch.search-after-page-size");
            ReflectionTestUtils.setField(searchAfterService, "env", env);
            ReflectionTestUtils.setField(searchAfterService, "highLevelClient", highLevelClientMock);
            when(searchAfterService.buildSearchAfterSearchRequest(any(),any(),any())).thenCallRealMethod();
            when(searchAfterService.executeSearchRequest(any())).thenCallRealMethod();
            when(searchAfterService.search(any(SearchRequest.class), any(RequestOptions.class))).thenReturn(mockedResponse);

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
            assertTrue("sorts should be empty", (sorts==null || sorts.size()==0));

            org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

            log.info("Result list size: " + hits.length);

            assertTrue("Result list should be empty", (hits.length==0));

        }
        catch (Exception e){
            assertFalse("Exception: " + e.getMessage(),true);
        }
    }


    // test utility method for creating SearchResponse
    private SearchResponse getTestSearchResponse(int hitCount){

        // create array of hits
        SearchHit[] testHits = new SearchHit[hitCount];
        for (int i=0; i<hitCount; i++){
            testHits[i] = new SearchHit(i);
            //  put in some sort values so new page can be searched for
            testHits[i].sortValues(
                    new Object[]{"111"},
                    new DocValueFormat[]{DocValueFormat.RAW}
                    );
        }

        // create number of totalHits
        int totalHits = (hitCount<=10000?hitCount:10000);

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

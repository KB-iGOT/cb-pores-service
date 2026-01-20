package com.igot.cb.pores.elasticsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.elasticsearch.indices.GetIndexRequest;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import com.igot.cb.pores.elasticsearch.dto.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EsUtilServiceImplMethodTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private GetIndexResponse getIndexResponse;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EsUtilServiceImpl esUtilService;

    private SearchCriteria searchCriteria;

    @BeforeEach
    void setUp() {
        searchCriteria = new SearchCriteria();
        searchCriteria.setPageNumber(0);
        searchCriteria.setPageSize(10);
        searchCriteria.setRequestedFields(List.of("field1", "field2"));
        Map<String, Object> filterCriteriaMap = new HashMap<>();
        searchCriteria.setFilterCriteriaMap((HashMap<String, Object>) filterCriteriaMap);
    }

    @Test
    void testAddRequestedFieldsToSearchSourceBuilder_emptyFields() throws Exception {
        SearchRequest.Builder builder = new SearchRequest.Builder();
        searchCriteria.setRequestedFields(new ArrayList<>());

        Method method = EsUtilServiceImpl.class.getDeclaredMethod("addRequestedFieldsToSearchSourceBuilder", SearchCriteria.class, SearchRequest.Builder.class);
        method.setAccessible(true);
        method.invoke(esUtilService, searchCriteria, builder);

        SearchRequest request = builder.build();
        assertNotNull(request.source(), "SearchRequest.source should not be null");
        assertNotNull(request.source().filter(), "Source.filter should not be null");
        List<String> includes = request.source().filter().includes();
        assertNotNull(includes, "Source.filter.includes should not be null");
        assertTrue(includes.isEmpty(), "Includes list should be empty when requestedFields is empty");
    }

    @Test
    void testAddFacetsToSearchSourceBuilder() throws Exception {
        SearchRequest.Builder builder = new SearchRequest.Builder();
        List<String> facets = List.of("communityId");

        Method method = EsUtilServiceImpl.class.getDeclaredMethod("addFacetsToSearchSourceBuilder", List.class, SearchRequest.Builder.class);
        method.setAccessible(true);
        method.invoke(esUtilService, facets, builder);

        SearchRequest request = builder.build();
        assertNotNull(request.aggregations(), "Aggregations should not be null when facets provided");
        assertEquals(1, request.aggregations().size(), "There should be one aggregation entry");
        assertTrue(request.aggregations().containsKey("communityId_agg"), "Expected aggregation key missing");

        Aggregation aggregation =
                request.aggregations().get("communityId_agg");
        assertNotNull(aggregation, "Aggregation object should not be null");
        assertNotNull(aggregation.terms(), "Terms aggregation should be present for the facet");
        assertEquals("communityId.keyword", aggregation.terms().field(), "Terms aggregation field mismatch");
        assertEquals(Integer.valueOf(250), aggregation.terms().size(), "Terms aggregation size mismatch");
    }

    @Test
    void testIsIndexPresent_IndexExists() throws Exception {
        when(elasticsearchClient.indices().get(any(GetIndexRequest.class)))
                .thenReturn(getIndexResponse);

        boolean result = esUtilService.isIndexPresent("test-index");

        assertTrue(result);
        verify(elasticsearchClient.indices()).get(any(GetIndexRequest.class));
    }

    @Test
    void testIsIndexPresent_IndexThrowsIOException() throws Exception {
        when(elasticsearchClient.indices().get(any(GetIndexRequest.class)))
                .thenThrow(new IOException("Simulated failure"));

        boolean result = esUtilService.isIndexPresent("test-index");

        assertFalse(result);
        verify(elasticsearchClient.indices()).get(any(GetIndexRequest.class));
    }
}
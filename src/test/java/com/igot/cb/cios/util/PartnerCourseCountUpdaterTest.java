package com.igot.cb.cios.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.contentpartner.service.ContentPartnerService;
import com.igot.cb.contentpartner.service.ContentPartnerServiceV2;
import com.igot.cb.pores.util.ApiResponse;
import com.igot.cb.pores.util.CbServerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartnerCourseCountUpdaterTest {

    private static final String SEARCH_URL = "http://cios/search";

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private CbServerProperties cbServerProperties;
    @Mock
    private ContentPartnerService contentPartnerService;
    @Mock
    private ContentPartnerServiceV2 contentPartnerServiceV2;

    private PartnerCourseCountUpdater updater;

    @BeforeEach
    void setUp() {
        lenient().when(cbServerProperties.getCiosContentServiceHost()).thenReturn("http://cios");
        lenient().when(cbServerProperties.getCiosContentServiceSearchApiUrl()).thenReturn("/search");
        updater = new PartnerCourseCountUpdater(mapper, restTemplate, cbServerProperties, contentPartnerService,
                contentPartnerServiceV2);
    }

    private void searchReturns(String json) throws Exception {
        when(restTemplate.postForObject(eq(SEARCH_URL), any(), eq(JsonNode.class))).thenReturn(mapper.readTree(json));
    }

    private void partnerIs(Map<String, Object> result) {
        ApiResponse response = new ApiResponse();
        response.setResult(result);
        when(contentPartnerService.getContentDetailsByPartnerCode("CODE")).thenReturn(response);
    }

    @Test
    void writesTotalDraftAndLiveCountsOntoThePartner() throws Exception {
        searchReturns("""
                {"totalCount":7,"facets":{"status":[{"value":"Draft","count":3},{"value":"live","count":4},
                 {"value":"other","count":9}]}}
                """);
        partnerIs(Map.of("id", "p1", "data", Map.of("name", "Coursera"),
                "transformContentViaApi", java.util.List.of()));

        updater.updateCourseCounts("CODE");

        ArgumentCaptor<JsonNode> saved = ArgumentCaptor.forClass(JsonNode.class);
        verify(contentPartnerServiceV2).createOrUpdate(saved.capture());
        JsonNode data = saved.getValue().path("data");
        assertEquals(7, data.path("totalCoursesCount").asLong());
        assertEquals(3, data.path("draftCoursesCount").asLong());
        assertEquals(4, data.path("liveCoursesCount").asLong());
        assertEquals("Coursera", data.path("name").asText());
        assertEquals("p1", saved.getValue().path("id").asText());
        assertEquals(true, saved.getValue().has("transformContentViaApi"));
    }

    @Test
    void partnerUpdateThatSucceeds_isNotReportedAsAnError() throws Exception {
        searchReturns("{\"totalCount\":1,\"facets\":{\"status\":[{\"value\":\"live\",\"count\":1}]}}");
        partnerIs(Map.of("id", "p1", "data", Map.of("name", "Coursera")));
        ApiResponse accepted = new ApiResponse();
        accepted.setResponseCode(org.springframework.http.HttpStatus.OK);
        when(contentPartnerServiceV2.createOrUpdate(any())).thenReturn(accepted);

        updater.updateCourseCounts("CODE");

        verify(contentPartnerServiceV2).createOrUpdate(any());
    }

    @Test
    void searchesOnlyThePartnersCoursesForTheirStatusCounts() throws Exception {
        searchReturns("{\"totalCount\":0,\"facets\":{\"status\":[]}}");
        partnerIs(Map.of("id", "p1", "data", Map.of()));

        updater.updateCourseCounts("CODE");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<JsonNode>> request = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(eq(SEARCH_URL), request.capture(), eq(JsonNode.class));
        JsonNode payload = request.getValue().getBody();
        assertEquals("CODE", payload.path("filterCriteriaMap").path("partnerCode").asText());
        assertEquals("status", payload.path("facets").get(0).asText());
        assertEquals(1, payload.path("pageSize").asInt());
    }

    @Test
    void statusMissingFromTheFacets_countsAsZero() throws Exception {
        searchReturns("{\"totalCount\":2,\"facets\":{\"status\":[{\"value\":\"draft\",\"count\":2}]}}");
        partnerIs(Map.of("id", "p1", "data", Map.of()));

        updater.updateCourseCounts("CODE");

        ArgumentCaptor<JsonNode> saved = ArgumentCaptor.forClass(JsonNode.class);
        verify(contentPartnerServiceV2).createOrUpdate(saved.capture());
        assertEquals(0, saved.getValue().path("data").path("liveCoursesCount").asLong());
    }

    @Test
    void incompleteSearchResponse_leavesThePartnerAlone() throws Exception {
        searchReturns("{\"totalCount\":2}");

        updater.updateCourseCounts("CODE");

        verifyNoInteractions(contentPartnerService, contentPartnerServiceV2);
    }

    @Test
    void nullSearchResponse_leavesThePartnerAlone() {
        when(restTemplate.postForObject(eq(SEARCH_URL), any(), eq(JsonNode.class))).thenReturn(null);

        updater.updateCourseCounts("CODE");

        verifyNoInteractions(contentPartnerService, contentPartnerServiceV2);
    }

    @Test
    void partnerThatCannotBeRead_isNotUpdated() throws Exception {
        searchReturns("{\"totalCount\":1,\"facets\":{\"status\":[]}}");
        when(contentPartnerService.getContentDetailsByPartnerCode("CODE")).thenReturn(null);

        updater.updateCourseCounts("CODE");

        verify(contentPartnerServiceV2, never()).createOrUpdate(any());
    }

    @Test
    void partnerUpdateThatIsRefused_isLoggedAndNeverFailsTheCall() throws Exception {
        searchReturns("{\"totalCount\":1,\"facets\":{\"status\":[{\"value\":\"draft\",\"count\":1}]}}");
        partnerIs(Map.of("id", "p1", "data", Map.of("protocol", "LTI")));
        ApiResponse refused = new ApiResponse();
        refused.setResponseCode(org.springframework.http.HttpStatus.BAD_REQUEST);
        refused.getParams().setErrMsg("not allowed");
        when(contentPartnerServiceV2.createOrUpdate(any())).thenReturn(refused);

        updater.updateCourseCounts("CODE");

        verify(contentPartnerServiceV2).createOrUpdate(any());
    }

    @Test
    void ltiPartner_isWrittenThroughTheV2PartnerServiceWithItsLtiFieldsIntact() throws Exception {
        searchReturns("{\"totalCount\":1,\"facets\":{\"status\":[{\"value\":\"draft\",\"count\":1}]}}");
        partnerIs(Map.of("id", "p1", "data", Map.of("protocol", "LTI", "ltiConfiguration", Map.of("clientId", "c"))));

        updater.updateCourseCounts("CODE");

        ArgumentCaptor<JsonNode> saved = ArgumentCaptor.forClass(JsonNode.class);
        verify(contentPartnerServiceV2).createOrUpdate(saved.capture());
        assertEquals("LTI", saved.getValue().path("data").path("protocol").asText());
        assertEquals("c", saved.getValue().path("data").path("ltiConfiguration").path("clientId").asText());
        assertEquals(1, saved.getValue().path("data").path("draftCoursesCount").asLong());
        verify(contentPartnerService, never()).createOrUpdate(any());
    }

    @Test
    void failureWhileRefreshing_isSwallowedBecauseTheCourseIsAlreadySaved() {
        when(restTemplate.postForObject(eq(SEARCH_URL), any(), eq(JsonNode.class)))
                .thenThrow(new IllegalStateException("search is down"));

        updater.updateCourseCounts("CODE");

        verifyNoInteractions(contentPartnerService, contentPartnerServiceV2);
    }
}

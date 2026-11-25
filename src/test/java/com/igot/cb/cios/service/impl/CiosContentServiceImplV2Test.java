package com.igot.cb.cios.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.cios.dto.OnboardContentV2Request;
import com.igot.cb.cios.util.CoursePublisher;
import com.igot.cb.cios.util.LtiContentFieldsHelper;
import com.igot.cb.cios.util.LtiContentOnboardTestProperties;
import com.igot.cb.cios.util.PartnerCourseCountUpdater;
import com.igot.cb.contentpartner.service.ContentPartnerService;
import com.igot.cb.pores.exceptions.CustomException;
import com.igot.cb.pores.util.ApiResponse;
import com.igot.cb.pores.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CiosContentServiceImplV2Test {

    private static final String PARTNER_CODE = "IGOT-COURSERATESTP-6OS93";
    private static final String PARTNER_ID = "8f8e63ca-d862-4a18-9116-b4383e4bd777";
    private static final String MAPPED_ITEM = """
            {"content":{"name":"Data Analysis with R Programming","description":"A course description",
             "redirectUrl":"https://www.coursera.org/api/rest/v1/tool-lti/launch","externalId":"joRZVwp9Eeun_RJEc0KNDw",
             "providerCustomClaim":{"link_type":"COURSE","course_id":"joRZVwp9Eeun_RJEc0KNDw"}}}
            """;

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private ContentPartnerService contentPartnerService;
    @Mock
    private CoursePublisher coursePublisher;
    @Mock
    private PartnerCourseCountUpdater countUpdater;

    private CiosContentServiceImplV2 service;

    @BeforeEach
    void setUp() {
        service = new CiosContentServiceImplV2(contentPartnerService,
                new LtiContentFieldsHelper(mapper, LtiContentOnboardTestProperties.packaged()), coursePublisher, countUpdater, mapper);
    }

    private ApiResponse partnerResponse() throws Exception {
        ObjectNode partner = mapper.createObjectNode();
        partner.put("id", PARTNER_ID);
        partner.set("data", mapper.readTree("""
                {"partnerCode":"IGOT-COURSERATESTP-6OS93","contentPartnerName":"COURSERA_TESTP",
                 "link":"https://portal.example.org/logo.png","isActive":true,"websiteUrl":"https://moodle.example.org/"}
                """));
        ApiResponse response = new ApiResponse();
        response.setResponseCode(HttpStatus.OK);
        response.setResult(mapper.convertValue(partner, Map.class));
        return response;
    }

    private OnboardContentV2Request request(String status) throws Exception {
        OnboardContentV2Request request = new OnboardContentV2Request();
        request.setPartnerCode(PARTNER_CODE);
        request.setStatus(status);
        request.setContentData(mapper.readTree(MAPPED_ITEM));
        request.setCourseType("paid");
        request.setRequiredKarmaPoints(50);
        request.setCompetenciesV6(mapper.readTree("[{\"competencyAreaName\":\"Behavioural\"}]"));
        return request;
    }

    private void partnerExists() throws Exception {
        when(contentPartnerService.getContentDetailsByPartnerCode(PARTNER_CODE)).thenReturn(partnerResponse());
    }

    private ObjectNode savedAsDraft() {
        ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.captor();
        verify(coursePublisher).saveAsDraft(captor.capture(), any(Timestamp.class));
        return captor.getValue();
    }

    private ObjectNode published() {
        ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.captor();
        verify(coursePublisher).publishAsLive(captor.capture(), any(JsonNode.class), any(Timestamp.class));
        return captor.getValue();
    }

    @Test
    void draft_buildsTheContentFromTheSpecAndSavesItAsADraft() throws Exception {
        partnerExists();

        ApiResponse response = service.onboardContent(request("draft"));

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertEquals(Constants.SUCCESS, response.getParams().getStatus());
        assertEquals(Constants.API_CIOS_CURATION_CREATE, response.getId());
        assertEquals(1, response.getResult().size());
        assertEquals(String.format(Constants.COURSES_PUBLISHED_MESSAGE, 0, 1),
                response.getResult().get(Constants.ONBOARD_RESULT_MESSAGE_KEY));

        JsonNode content = savedAsDraft().path("content");
        assertEquals("Data Analysis with R Programming", content.path("name").asText());
        assertEquals("A course description", content.path("description").asText());
        assertEquals("joRZVwp9Eeun_RJEc0KNDw", content.path("providerCustomClaim").path("course_id").asText());
        assertEquals("draft", content.path("status").asText());
        assertTrue(content.path("contentId").asText().startsWith("ext_"));
        assertEquals(PARTNER_ID, content.path("contentPartner").path("id").asText());
        assertEquals(PARTNER_CODE, content.path("contentPartner").path("partnerCode").asText());
        assertEquals("COURSERA_TESTP", content.path("contentPartner").path("contentPartnerName").asText());
        assertEquals("https://portal.example.org/logo.png", content.path("contentPartner").path("link").asText());
        verify(coursePublisher, never()).publishAsLive(any(), any(), any());
        verify(countUpdater).updateCourseCounts(PARTNER_CODE);
    }

    @Test
    void missingMandatoryFields_areCompletedAndTheResponseDoesNotListThem() throws Exception {
        partnerExists();

        ApiResponse response = service.onboardContent(request("draft"));

        assertFalse(response.getResult().containsKey("defaultedFields"));
        JsonNode content = savedAsDraft().path("content");
        assertFalse(content.path("objectives").asText().isBlank());
        assertEquals("paid", content.path("courseType").asText());
        assertEquals(50, content.path("requiredKarmaPoints").asInt());
        assertEquals("3600", content.path("duration").asText());
        assertEquals("General", content.path("topic").asText());
        assertFalse(content.has("summary"));
        assertFalse(content.has("source"));
    }

    @Test
    void live_isPublishedWithThePartnerDataAndTheV1StyleMessageComesBack() throws Exception {
        partnerExists();

        ApiResponse response = service.onboardContent(request("live"));

        assertEquals(HttpStatus.OK, response.getResponseCode());
        published();
        assertEquals(Constants.API_CIOS_CURATION_CREATE, response.getId());
        assertEquals(1, response.getResult().size());
        assertEquals(String.format(Constants.COURSES_PUBLISHED_MESSAGE, 1, 1),
                response.getResult().get(Constants.ONBOARD_RESULT_MESSAGE_KEY));
        verify(coursePublisher).publishAsLive(any(), eq(partnerData()), any());
        verify(coursePublisher, never()).saveAsDraft(any(), any());
        verify(countUpdater).updateCourseCounts(PARTNER_CODE);
    }

    private JsonNode partnerData() throws Exception {
        return mapper.convertValue(partnerResponse().getResult(), JsonNode.class).path("data");
    }

    @Test
    void publishRefusedWithAReason_isABadRequestWithThatReasonAndNoCountRefresh() throws Exception {
        partnerExists();
        doThrow(new CustomException(Constants.ERROR, "courseEnrolLimit is above the partner's overAllLimit of 5",
                HttpStatus.BAD_REQUEST)).when(coursePublisher).publishAsLive(any(), any(), any());

        ApiResponse response = service.onboardContent(request("live"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals("courseEnrolLimit is above the partner's overAllLimit of 5", response.getParams().getErrMsg());
        verifyNoInteractions(countUpdater);
    }

    @Test
    void statusInAnyCase_isStoredInLowerCase() throws Exception {
        partnerExists();

        service.onboardContent(request("  DRAFT "));

        assertEquals("draft", savedAsDraft().path("content").path("status").asText());
    }

    @Test
    void requestExtras_winOverEverythingElseAndTagsBecomeSearchTags() throws Exception {
        partnerExists();
        OnboardContentV2Request request = request("draft");
        request.setCourseType("free");
        request.setCourseEnrolLimit(5);
        request.setRequiredKarmaPoints(10);
        request.setTags(List.of("A", "b"));

        service.onboardContent(request);

        JsonNode content = savedAsDraft().path("content");
        assertEquals("free", content.path("courseType").asText());
        assertEquals(5, content.path("courseEnrolLimit").asInt());
        assertEquals(10, content.path("requiredKarmaPoints").asInt());
        assertEquals(mapper.readTree("[\"data analysis with r programming\",\"a\",\"b\"]"),
                content.path("searchTags"));
    }

    @Test
    void contentPartnerSentByTheRequest_isSavedAsSent() throws Exception {
        partnerExists();
        OnboardContentV2Request request = request("draft");
        request.setContentPartner(mapper.readTree(
                "{\"id\":\"" + PARTNER_ID + "\",\"partnerCode\":\"LEGACY CODE\",\"isActive\":true}"));

        service.onboardContent(request);

        JsonNode partner = savedAsDraft().path("content").path("contentPartner");
        assertEquals("LEGACY CODE", partner.path("partnerCode").asText());
        assertFalse(partner.has("contentPartnerName"));
        assertFalse(partner.has("link"));
    }

    @Test
    void contentPartnerSentWithoutIdOrCode_isABadRequest() throws Exception {
        partnerExists();
        OnboardContentV2Request request = request("draft");
        request.setContentPartner(mapper.readTree("{\"id\":\"" + PARTNER_ID + "\"}"));

        ApiResponse response = service.onboardContent(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.CONTENT_PARTNER_INCOMPLETE, response.getParams().getErrMsg());
        verifyNoInteractions(coursePublisher);
    }

    @Test
    void contentPartnerOfAnotherPartner_isABadRequest() throws Exception {
        partnerExists();
        OnboardContentV2Request request = request("live");
        request.setContentPartner(mapper.readTree("{\"id\":\"someone-else\",\"partnerCode\":\"X\"}"));

        ApiResponse response = service.onboardContent(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.CONTENT_PARTNER_MISMATCH + PARTNER_CODE, response.getParams().getErrMsg());
        verifyNoInteractions(coursePublisher, countUpdater);
    }

    @Test
    void requestWithoutThePublishFormFields_isABadRequestForDraftAndLiveAndAddsNothing() throws Exception {
        partnerExists();
        for (String status : List.of("draft", "live")) {
            OnboardContentV2Request request = request(status);
            request.setCourseType(null);
            request.setRequiredKarmaPoints(null);
            request.setCompetenciesV6(null);

            ApiResponse response = service.onboardContent(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
            assertEquals(Constants.MANDATORY_FIELDS_MISSING + "courseType, competencies_v6",
                    response.getParams().getErrMsg());
        }
        verifyNoInteractions(coursePublisher, countUpdater);
    }

    @Test
    void paidCourseWithoutKarmaPoints_isABadRequestButAFreeCourseIsNot() throws Exception {
        partnerExists();
        OnboardContentV2Request paid = request("draft");
        paid.setRequiredKarmaPoints(null);
        OnboardContentV2Request free = request("draft");
        free.setCourseType("free");
        free.setRequiredKarmaPoints(null);

        ApiResponse paidResponse = service.onboardContent(paid);
        ApiResponse freeResponse = service.onboardContent(free);

        assertEquals(HttpStatus.BAD_REQUEST, paidResponse.getResponseCode());
        assertEquals(Constants.MANDATORY_FIELDS_MISSING + "requiredKarmaPoints", paidResponse.getParams().getErrMsg());
        assertEquals(HttpStatus.OK, freeResponse.getResponseCode());
    }

    @Test
    void publishFormFieldsAlreadyInTheContent_countAsPresent() throws Exception {
        partnerExists();
        OnboardContentV2Request request = request("draft");
        request.setCourseType(null);
        request.setRequiredKarmaPoints(null);
        request.setCompetenciesV6(null);
        request.setContentData(mapper.readTree("""
                {"content":{"name":"N","externalId":"E1","redirectUrl":"https://a.b/c","courseType":"paid",
                 "requiredKarmaPoints":5,"competencies_v6":[{"competencyAreaName":"Functional"}]}}
                """));

        ApiResponse response = service.onboardContent(request);

        assertEquals(HttpStatus.OK, response.getResponseCode());
    }

    @Test
    void competenciesBadgeAndDifficultyFromTheRequest_areSavedWithTheCourse() throws Exception {
        partnerExists();
        OnboardContentV2Request request = request("draft");
        request.setCompetenciesV5(mapper.readTree("[{\"id\":5}]"));
        request.setBadgeDetailsV1(mapper.readTree("{\"badge\":true}"));
        request.setDifficultyLevel("Beginner");

        service.onboardContent(request);

        JsonNode content = savedAsDraft().path("content");
        assertEquals(5, content.path("competencies_v5").get(0).path("id").asInt());
        assertEquals("Behavioural", content.path("competencies_v6").get(0).path("competencyAreaName").asText());
        assertTrue(content.path("badgeDetails_v1").path("badge").asBoolean());
        assertEquals("Beginner", content.path("difficultyLevel").asText());
    }

    @Test
    void transformFieldsTheContentLacks_getDefaultsEvenWithoutNameIdOrLink() throws Exception {
        partnerExists();
        OnboardContentV2Request request = request("draft");
        request.setContentData(mapper.readTree("{\"content\":{\"providerCustomClaim\":{\"course_id\":\"c1\"}}}"));

        ApiResponse response = service.onboardContent(request);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        JsonNode content = savedAsDraft().path("content");
        assertFalse(content.path("externalId").asText().isBlank());
        assertFalse(content.path("name").asText().isBlank());
        assertEquals("https://moodle.example.org/", content.path("redirectUrl").asText());
        assertEquals("c1", content.path("providerCustomClaim").path("course_id").asText());
    }

    @Test
    void aV1StyleRequestWithoutATopLevelPartnerCode_usesThePartnerCodeInsideContentPartner() throws Exception {
        partnerExists();
        OnboardContentV2Request request = request("live");
        request.setPartnerCode(null);
        request.setContentPartner(mapper.readTree(
                "{\"id\":\"" + PARTNER_ID + "\",\"partnerCode\":\"" + PARTNER_CODE + "\",\"providerTips\":[]}"));

        ApiResponse response = service.onboardContent(request);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        verify(coursePublisher).publishAsLive(any(), any(JsonNode.class), any(Timestamp.class));
        verify(countUpdater).updateCourseCounts(PARTNER_CODE);
    }

    @Test
    void invalidStatus_isRejectedBeforeAnythingIsRead() throws Exception {
        ApiResponse response = service.onboardContent(request("published"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.STATUS_NOT_VALID, response.getParams().getErrMsg());
        verifyNoInteractions(contentPartnerService, coursePublisher, countUpdater);
    }

    @Test
    void missingPartnerCode_isABadRequest() throws Exception {
        OnboardContentV2Request request = request("draft");
        request.setPartnerCode(" ");

        ApiResponse response = service.onboardContent(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.PARTNER_CODE_MISSING, response.getParams().getErrMsg());
    }

    @Test
    void contentDataThatIsMissingOrNotAnObject_isABadRequest() throws Exception {
        OnboardContentV2Request missing = request("draft");
        missing.setContentData(null);
        OnboardContentV2Request array = request("draft");
        array.setContentData(mapper.readTree("[]"));

        assertEquals(Constants.CONTENT_DATA_INVALID, service.onboardContent(missing).getParams().getErrMsg());
        assertEquals(HttpStatus.BAD_REQUEST, service.onboardContent(array).getResponseCode());
    }

    @Test
    void unknownPartner_isNotFound() throws Exception {
        ApiResponse notFound = new ApiResponse();
        notFound.setResponseCode(HttpStatus.BAD_REQUEST);
        when(contentPartnerService.getContentDetailsByPartnerCode(PARTNER_CODE)).thenReturn(notFound);

        ApiResponse response = service.onboardContent(request("draft"));

        assertEquals(HttpStatus.NOT_FOUND, response.getResponseCode());
        assertEquals(Constants.PARTNER_NOT_FOUND + PARTNER_CODE, response.getParams().getErrMsg());
        verifyNoInteractions(coursePublisher);
    }

    @Test
    void partnerReadThatFails_isAServerError() throws Exception {
        when(contentPartnerService.getContentDetailsByPartnerCode(PARTNER_CODE)).thenReturn(null);

        ApiResponse response = service.onboardContent(request("draft"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
    }

    @Test
    void contentDataWithoutAContentObject_isABadRequest() throws Exception {
        partnerExists();
        OnboardContentV2Request request = request("draft");
        request.setContentData(mapper.readTree("{\"other\":{\"name\":\"x\"}}"));

        ApiResponse response = service.onboardContent(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.NO_CONTENT_OBJECT, response.getParams().getErrMsg());
        verifyNoInteractions(coursePublisher);
    }

    @Test
    void theRequestItself_isNeverChangedByTheDefaultsAndPlatformFields() throws Exception {
        partnerExists();
        OnboardContentV2Request request = request("draft");
        JsonNode before = request.getContentData().deepCopy();

        service.onboardContent(request);

        assertEquals(before, request.getContentData());
    }

    @Test
    void failureWhileStoring_isAServerErrorWithTheCause() throws Exception {
        partnerExists();
        doThrow(new IllegalStateException("cios is down")).when(coursePublisher).saveAsDraft(any(), any());

        ApiResponse response = service.onboardContent(request("draft"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals(Constants.ONBOARD_FAILED + ": cios is down", response.getParams().getErrMsg());
        verifyNoInteractions(countUpdater);
    }
}

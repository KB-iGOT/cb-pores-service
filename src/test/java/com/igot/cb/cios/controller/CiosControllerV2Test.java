package com.igot.cb.cios.controller;

import com.igot.cb.cios.dto.OnboardContentV2Request;
import com.igot.cb.cios.service.CiosContentServiceV2;
import com.igot.cb.pores.util.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CiosControllerV2Test {

    @Mock
    private CiosContentServiceV2 ciosContentServiceV2;

    @InjectMocks
    private CiosControllerV2 controller;

    @Test
    void onboardContent_returnsTheServiceResponseWithItsOwnStatus() {
        OnboardContentV2Request request = new OnboardContentV2Request();
        ApiResponse failed = new ApiResponse();
        failed.setResponseCode(HttpStatus.BAD_REQUEST);
        when(ciosContentServiceV2.onboardContent(request)).thenReturn(failed);

        ResponseEntity<ApiResponse> result = controller.onboardContent(request);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertSame(failed, result.getBody());
    }

    @Test
    void onboardContent_returnsOkForASuccessfulOnboard() {
        OnboardContentV2Request request = new OnboardContentV2Request();
        ApiResponse ok = new ApiResponse();
        ok.setResponseCode(HttpStatus.OK);
        when(ciosContentServiceV2.onboardContent(request)).thenReturn(ok);

        assertEquals(HttpStatus.OK, controller.onboardContent(request).getStatusCode());
    }

    @Test
    void requestBody_isBoundFromJsonIncludingTheMappedContentDataAndTheUnderscoreFields() throws Exception {
        ApiResponse ok = new ApiResponse();
        ok.setResponseCode(HttpStatus.OK);
        when(ciosContentServiceV2.onboardContent(org.mockito.ArgumentMatchers.any())).thenReturn(ok);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(post("/cios/v2/LTI/onboardContent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"partnerCode":"IGOT-COURSERATESTP-6OS93","status":"live",
                                 "courseType":"paid","courseEnrolLimit":100,"requiredKarmaPoints":50,
                                 "tags":["data"],"accessSettingsEnabled":true,
                                 "competencies_v6":[{"competencyAreaName":"Behavioural"}],
                                 "badgeDetails_v1":[{"badgeId":"b1"}],
                                 "contentPartner":{"id":"p1","partnerCode":"CODE"},
                                 "contentData":{"content":{"name":"Data Analysis with R Programming",
                                   "externalId":"joRZVwp9Eeun_RJEc0KNDw",
                                   "redirectUrl":"https://www.coursera.org/api/rest/v1/tool-lti/launch",
                                   "providerCustomClaim":{"course_id":"joRZVwp9Eeun_RJEc0KNDw"}}}}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<OnboardContentV2Request> captor = ArgumentCaptor.forClass(OnboardContentV2Request.class);
        verify(ciosContentServiceV2).onboardContent(captor.capture());
        OnboardContentV2Request bound = captor.getValue();
        assertEquals("IGOT-COURSERATESTP-6OS93", bound.getPartnerCode());
        assertEquals("live", bound.getStatus());
        assertEquals("paid", bound.getCourseType());
        assertEquals(100, bound.getCourseEnrolLimit());
        assertEquals(50, bound.getRequiredKarmaPoints());
        assertEquals(true, bound.isAccessSettingsEnabled());
        assertEquals("Behavioural", bound.getCompetenciesV6().get(0).path("competencyAreaName").asText());
        assertEquals("b1", bound.getBadgeDetailsV1().get(0).path("badgeId").asText());
        assertEquals("p1", bound.getContentPartner().path("id").asText());
        assertEquals("Data Analysis with R Programming", bound.getContentData().path("content").path("name").asText());
        assertEquals("joRZVwp9Eeun_RJEc0KNDw", bound.getContentData().path("content").path("externalId").asText());
        assertEquals("joRZVwp9Eeun_RJEc0KNDw",
                bound.getContentData().path("content").path("providerCustomClaim").path("course_id").asText());
    }

    @Test
    void theOldFieldNameContentItem_isNotBoundSoTheServiceRejectsTheRequest() throws Exception {
        ApiResponse ok = new ApiResponse();
        ok.setResponseCode(HttpStatus.OK);
        when(ciosContentServiceV2.onboardContent(org.mockito.ArgumentMatchers.any())).thenReturn(ok);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(post("/cios/v2/LTI/onboardContent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"partnerCode\":\"CODE\",\"status\":\"draft\",\"contentItem\":{\"content\":{\"name\":\"x\"}}}"))
                .andExpect(status().isOk());

        ArgumentCaptor<OnboardContentV2Request> captor = ArgumentCaptor.forClass(OnboardContentV2Request.class);
        verify(ciosContentServiceV2).onboardContent(captor.capture());
        assertEquals(null, captor.getValue().getContentData());
    }

    @Test
    void theV1RequestBodyShape_isAcceptedWithoutATopLevelPartnerCode() throws Exception {
        ApiResponse ok = new ApiResponse();
        ok.setResponseCode(HttpStatus.OK);
        when(ciosContentServiceV2.onboardContent(org.mockito.ArgumentMatchers.any())).thenReturn(ok);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(post("/cios/v2/LTI/onboardContent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"live",
                                 "contentPartner":{"contentPartnerName":"COURSERA_TESTP","partnerCode":"IGOT-COURSERATESTP-6OS93",
                                   "id":"8f8e63ca-d862-4a18-9116-b4383e4bd777","providerTips":[],"isActive":true},
                                 "contentData":{"content":{"externalId":"90","partnerId":"8f8e63ca","fileId":"f1","topic":"DPDP",
                                   "duration":"5400","source":"SampleFile.csv","name":"Digital Personal Data Protection update 1",
                                   "redirectUrl":"http://stg-me.sparkl.training/org/karmayogi/course/86",
                                   "objectives":"<ul><li>x</li></ul>","description":"d","courseType":"paid",
                                   "contentSearchTags":["digital personal data protection update 1"],
                                   "contentId":"ext_11470813541817548813","courseEnrolLimit":0,"requiredKarmaPoints":0,
                                   "difficultyLevel":"Beginner","summary":""}},
                                 "competencies_v6":[{"competencyAreaName":"Functional"}],
                                 "tags":[],"accessSettingsEnabled":false,"difficultyLevel":"Beginner",
                                 "courseType":"paid","courseEnrolLimit":0,"requiredKarmaPoints":0}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<OnboardContentV2Request> captor = ArgumentCaptor.forClass(OnboardContentV2Request.class);
        verify(ciosContentServiceV2).onboardContent(captor.capture());
        OnboardContentV2Request bound = captor.getValue();
        assertEquals("IGOT-COURSERATESTP-6OS93", bound.getPartnerCode());
        assertEquals("live", bound.getStatus());
        assertEquals("90", bound.getContentData().path("content").path("externalId").asText());
        assertEquals("ext_11470813541817548813", bound.getContentData().path("content").path("contentId").asText());
        assertEquals("Functional", bound.getCompetenciesV6().get(0).path("competencyAreaName").asText());
        assertEquals(0, bound.getRequiredKarmaPoints());
    }
}

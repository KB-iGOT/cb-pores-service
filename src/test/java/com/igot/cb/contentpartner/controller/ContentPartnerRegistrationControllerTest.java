package com.igot.cb.contentpartner.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.contentpartner.service.ContentPartnerRegistrationService;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import com.igot.cb.pores.util.ApiResponse;
import com.igot.cb.pores.util.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@ExtendWith(MockitoExtension.class)
class ContentPartnerRegistrationControllerTest {

    @Mock
    private ContentPartnerRegistrationService partnerService;

    @InjectMocks
    private ContentPartnerRegistrationController controller;

    private final ObjectMapper mapper = new ObjectMapper();
    private final String token = "dummy-token";

    // -------------------------------
    // CREATE TEST CASES
    // -------------------------------
    @Test
    void testCreate_Success() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        JsonNode requestJson = mapper.readTree(
                "{\"contentPartnerName\":\"Org1\",\"email\":\"org@gmail.com\"}"
        );

        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.OK);

        when(partnerService.update(any(), anyString())).thenReturn(mockResponse);

        mockMvc.perform(post("/contentpartner/register/v1/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(Constants.X_AUTH_TOKEN, token)
                        .content(requestJson.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void testCreate_Failure() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        JsonNode requestJson = mapper.readTree("{\"contentPartnerName\":\"Org1\"}");

        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.BAD_REQUEST);
        mockResponse.getParams().setErrMsg("Validation error");

        when(partnerService.update(any(), anyString())).thenReturn(mockResponse);

        mockMvc.perform(post("/contentpartner/register/v1/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(Constants.X_AUTH_TOKEN, token)
                        .content(requestJson.toString()))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------
    // UPDATE TEST CASES
    // -------------------------------
    @Test
    void testUpdate_Success() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        JsonNode requestJson = mapper.readTree("{\"id\":\"123\",\"status\":\"APPROVED\"}");

        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.OK);

        when(partnerService.update(any(), anyString())).thenReturn(mockResponse);

        mockMvc.perform(post("/contentpartner/register/v1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(Constants.X_AUTH_TOKEN, token)
                        .content(requestJson.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdate_Failure() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        JsonNode requestJson = mapper.readTree("{\"id\":\"123\",\"status\":\"INVALID\"}");

        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.BAD_REQUEST);

        when(partnerService.update(any(), anyString())).thenReturn(mockResponse);

        mockMvc.perform(post("/contentpartner/register/v1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(Constants.X_AUTH_TOKEN, token)
                        .content(requestJson.toString()))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------
    // READ TEST CASES
    // -------------------------------
    @Test
    void testRead_Success() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        String id = "test-id-123";

        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.OK);
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("contentPartnerName", "Org1");
        mockResponse.setResult(result);

        when(partnerService.read(eq(id), anyString())).thenReturn(mockResponse);

        mockMvc.perform(get("/contentpartner/register/v1/read/" + id)
                        .header(Constants.X_AUTH_TOKEN, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(id))
                .andExpect(jsonPath("$.responseCode").value("OK"));

        verify(partnerService, times(1)).read(eq(id), anyString());
    }

    @Test
    void testRead_NotFound() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        String id = "non-existent-id";

        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.BAD_REQUEST);
        mockResponse.getParams().setErrMsg("Invalid ID");

        when(partnerService.read(eq(id), anyString())).thenReturn(mockResponse);

        mockMvc.perform(get("/contentpartner/register/v1/read/" + id)
                        .header(Constants.X_AUTH_TOKEN, token))
                .andExpect(status().isOk());

        verify(partnerService, times(1)).read(eq(id), anyString());
    }

    // -------------------------------
    // SEARCH TEST CASE
    // -------------------------------
    @Test
    void testSearch_Success() throws Exception {

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        SearchCriteria criteria = new SearchCriteria();
        criteria.setSearchString("Org");

        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.OK);

        when(partnerService.searchEntity(any(), anyString())).thenReturn(mockResponse);

        mockMvc.perform(post("/contentpartner/register/v1/search")
                        .header(Constants.X_AUTH_TOKEN, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(criteria)))
                .andExpect(status().isOk());
    }
}
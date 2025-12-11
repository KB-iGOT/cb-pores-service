package com.igot.cb.contentpartner.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.contentpartner.service.ContentPartnerRegistrationService;
import com.igot.cb.pores.util.ApiResponse;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ExtendWith(MockitoExtension.class)
class ContentPartnerRegistrationControllerTest {

    @Mock
    private ContentPartnerRegistrationService partnerService;

    @InjectMocks
    private ContentPartnerRegistrationController controller;

    private final ObjectMapper mapper = new ObjectMapper();


    // CREATE TEST CASES
    @Test
    void testCreate_Success() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        JsonNode requestJson = mapper.readTree("{\"contentPartnerName\":\"Org1\",\"email\":\"org@gmail.com\"}");

        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.OK);

        when(partnerService.createOrUpdate(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/contentpartnerregistration/v1/create")
                        .contentType(MediaType.APPLICATION_JSON)
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

        when(partnerService.createOrUpdate(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/contentpartnerregistration/v1/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson.toString()))
                .andExpect(status().isBadRequest());
    }


    // UPDATE TEST CASES
    @Test
    void testUpdate_Success() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        JsonNode requestJson = mapper.readTree("{\"id\":\"123\",\"status\":\"APPROVED\"}");

        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.OK);

        when(partnerService.createOrUpdate(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/contentpartnerregistration/v1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdate_Failure() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        JsonNode requestJson = mapper.readTree("{\"id\":\"123\",\"status\":\"INVALID\"}");

        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.BAD_REQUEST);
        mockResponse.getParams().setErrMsg("Invalid status");

        when(partnerService.createOrUpdate(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/contentpartnerregistration/v1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson.toString()))
                .andExpect(status().isBadRequest());
    }


    // READ TEST CASES
    @Test
    void testRead_Success() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        String id = "test-id-123";

        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.OK);
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("contentPartnerName", "Org1");
        result.put("email", "org1@gmail.com");
        mockResponse.setResult(result);

        when(partnerService.read(id)).thenReturn(mockResponse);

        mockMvc.perform(get("/contentpartnerregistration/v1/read/" + id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("OK"));

        verify(partnerService, times(1)).read(id);
    }

    @Test
    void testRead_NotFound() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        String id = "non-existent-id";

        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.BAD_REQUEST);
        mockResponse.getParams().setErrMsg("Invalid ID");
        mockResponse.getParams().setStatus("FAILED");

        when(partnerService.read(id)).thenReturn(mockResponse);

        mockMvc.perform(get("/contentpartnerregistration/v1/read/" + id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(partnerService, times(1)).read(id);
    }

    @Test
    void testRead_WithSpaceAsId() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        String id = " ";

        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.BAD_REQUEST);
        mockResponse.getParams().setErrMsg("ID not found");
        mockResponse.getParams().setStatus("FAILED");

        when(partnerService.read(id)).thenReturn(mockResponse);

        mockMvc.perform(get("/contentpartnerregistration/v1/read/" + id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(partnerService, times(1)).read(id);
    }

    @Test
    void testRead_InternalServerError() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        String id = "test-id-error";

        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
        mockResponse.getParams().setErrMsg("Database connection failed");
        mockResponse.getParams().setStatus("FAILED");

        when(partnerService.read(id)).thenReturn(mockResponse);

        mockMvc.perform(get("/contentpartnerregistration/v1/read/" + id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(partnerService, times(1)).read(id);
    }
}
package com.igot.cb.competencies.theme.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.competencies.theme.controller.CompetencyThemeController;
import com.igot.cb.competencies.theme.service.CompetencyThemeService;
import com.igot.cb.pores.dto.CustomResponse;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import com.igot.cb.pores.util.ApiResponse;
import com.igot.cb.pores.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class CompetencyThemeControllerTest {

    @Mock
    private CompetencyThemeService competencyThemeService;

    @InjectMocks
    private CompetencyThemeController competencyThemeController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testLoadCompetencyAreas_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.ms-excel", "dummy content".getBytes());

        doNothing().when(competencyThemeService).loadCompetencyTheme(any(), anyString());

        ResponseEntity<String> response = competencyThemeController.loadCompetencyAreas(file, "token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Loading of competencyTheme from excel is successful.", response.getBody());
        verify(competencyThemeService, times(1)).loadCompetencyTheme(any(), anyString());
    }

    @Test
    void testLoadCompetencyAreas_Failure() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.ms-excel", "dummy content".getBytes());

        doThrow(new RuntimeException("Error")).when(competencyThemeService).loadCompetencyTheme(any(), anyString());

        ResponseEntity<String> response = competencyThemeController.loadCompetencyAreas(file, "token");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Error during loading of competencyTheme from excel"));
    }

    @Test
    void testSearch() {
        SearchCriteria criteria = new SearchCriteria();
        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.OK);
        when(competencyThemeService.searchCompTheme(any())).thenReturn(mockResponse);

        ResponseEntity<?> response = competencyThemeController.search(criteria);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(competencyThemeService, times(1)).searchCompTheme(criteria);
    }

    @Test
    void testCreateCompetencyTheme() throws Exception {
        JsonNode jsonNode = objectMapper.readTree("{\"key\":\"value\"}");
        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.CREATED);
        when(competencyThemeService.createCompTheme(any(), anyString())).thenReturn(mockResponse);

        ResponseEntity<CustomResponse> response = competencyThemeController.createCompetencyTheme(jsonNode, "token");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(competencyThemeService, times(1)).createCompTheme(jsonNode, "token");
    }

    @Test
    void testUpdate() throws Exception {
        JsonNode jsonNode = objectMapper.readTree("{\"key\":\"value\"}");
        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.OK);
        when(competencyThemeService.updateCompTheme(any())).thenReturn(mockResponse);

        ResponseEntity<CustomResponse> response = competencyThemeController.update(jsonNode);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(competencyThemeService, times(1)).updateCompTheme(jsonNode);
    }

    @Test
    void testCompetencyThemeRead() {
        String id = "123";
        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.OK);
        when(competencyThemeService.readCompTheme(id)).thenReturn(mockResponse);

        ResponseEntity<?> response = competencyThemeController.competencyThemeRead(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(competencyThemeService, times(1)).readCompTheme(id);
    }

    @Test
    void testDeleteCompetencyArea() {
        String id = "123";
        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.OK);
        when(competencyThemeService.deleteCompetencyTheme(id)).thenReturn(mockResponse);

        ResponseEntity<CustomResponse> response = competencyThemeController.deleteCompetencyArea(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(competencyThemeService, times(1)).deleteCompetencyTheme(id);
    }

    @Test
    void testCreateTerm() throws Exception {
        JsonNode jsonNode = objectMapper.readTree("{\"key\":\"value\"}");
        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.CREATED);
        when(competencyThemeService.createTerm(any())).thenReturn(mockResponse);

        ResponseEntity<ApiResponse> response = competencyThemeController.createTerm(jsonNode);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(competencyThemeService, times(1)).createTerm(jsonNode);
    }
}


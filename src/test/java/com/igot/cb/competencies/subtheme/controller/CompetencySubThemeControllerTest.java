package com.igot.cb.competencies.subtheme.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.competencies.subtheme.service.CompetencySubThemeService;
import com.igot.cb.pores.dto.CustomResponse;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import com.igot.cb.pores.util.ApiResponse;
import com.igot.cb.pores.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompetencySubThemeControllerTest {

    @Mock
    private CompetencySubThemeService competencySubThemeService;

    @InjectMocks
    private CompetencySubThemeController controller;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mapper = new ObjectMapper();
    }

    @Test
    void testLoadCompetencyAreas_success() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.ms-excel", "test".getBytes());
        String token = "dummy-token";

        doNothing().when(competencySubThemeService).loadCompetencySubTheme(file, token);

        ResponseEntity<String> response = controller.loadCompetencyAreas(file, token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Loading of competencySubTheme from excel is successful.", response.getBody());
    }

    @Test
    void testLoadCompetencyAreas_exception() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.ms-excel", "test".getBytes());
        String token = "dummy-token";

        doThrow(new RuntimeException("Failed")).when(competencySubThemeService).loadCompetencySubTheme(file, token);

        ResponseEntity<String> response = controller.loadCompetencyAreas(file, token);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Error during loading of competencySubTheme from excel: Failed"));
    }

    @Test
    void testSearch() {
        SearchCriteria criteria = new SearchCriteria();
        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.OK);

        when(competencySubThemeService.searchCompSubTheme(criteria)).thenReturn(mockResponse);

        ResponseEntity<?> response = controller.search(criteria);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
    }

    @Test
    void testCreateCompetencySubTheme() {
        JsonNode jsonNode = mapper.createObjectNode();
        String token = "token";

        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.CREATED);

        when(competencySubThemeService.createCompSubTheme(jsonNode, token)).thenReturn(mockResponse);

        ResponseEntity<CustomResponse> response = controller.createCompetencySubTheme(jsonNode, token);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
    }

    @Test
    void testUpdate() {
        JsonNode jsonNode = mapper.createObjectNode();

        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.OK);

        when(competencySubThemeService.updateCompSubTheme(jsonNode)).thenReturn(mockResponse);

        ResponseEntity<CustomResponse> response = controller.update(jsonNode);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
    }

    @Test
    void testCompetencySubThemeRead() {
        String id = "123";

        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.OK);

        when(competencySubThemeService.readCompSubTheme(id)).thenReturn(mockResponse);

        ResponseEntity<?> response = controller.competencySubThemeRead(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
    }

    @Test
    void testDeleteCompetencySubTheme() {
        String id = "123";

        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.OK);

        when(competencySubThemeService.deleteCompetencySubTheme(id)).thenReturn(mockResponse);

        ResponseEntity<CustomResponse> response = controller.deleteCompetencySubTheme(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
    }

    @Test
    void testCreateTerm() {
        JsonNode jsonNode = mapper.createObjectNode();

        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.OK);

        when(competencySubThemeService.createTerm(jsonNode)).thenReturn(mockResponse);

        ResponseEntity<ApiResponse> response = controller.createTerm(jsonNode);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
    }
}

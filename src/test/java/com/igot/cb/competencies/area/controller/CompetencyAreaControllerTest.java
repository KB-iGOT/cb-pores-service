package com.igot.cb.competencies.area.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.competencies.area.service.CompetencyAreaService;
import com.igot.cb.pores.dto.CustomResponse;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CompetencyAreaControllerTest {

    @Mock
    private CompetencyAreaService competencyAreaService;

    @InjectMocks
    private CompetencyAreaController controller;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
    }

    @Test
    void testLoadCompetencyAreas_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.ms-excel", "data".getBytes());
        ResponseEntity<String> response = controller.loadCompetencyAreas(file, "token");

        verify(competencyAreaService, times(1)).loadCompetencyArea(file, "token");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("successful"));
    }

    @Test
    void testLoadCompetencyAreas_exception() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.ms-excel", "data".getBytes());
        doThrow(new RuntimeException("Error")).when(competencyAreaService).loadCompetencyArea(file, "token");

        ResponseEntity<String> response = controller.loadCompetencyAreas(file, "token");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Error"));
    }

    @Test
    void testCreateCompetencyArea() throws Exception {
        JsonNode node = objectMapper.readTree("{\"key\":\"value\"}");
        CustomResponse customResponse = new CustomResponse();
        customResponse.setResponseCode(HttpStatus.CREATED);
        customResponse.setMessage("Created");

        when(competencyAreaService.createCompArea(any(JsonNode.class), anyString())).thenReturn(customResponse);
        ResponseEntity<CustomResponse> response = controller.createCompetencyArea(node, "token");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Created", response.getBody().getMessage());
    }

    @Test
    void testUpdateCompetencyArea() throws Exception {
        JsonNode node = objectMapper.readTree("{\"id\":\"123\"}");
        CustomResponse customResponse = new CustomResponse();
        customResponse.setResponseCode(HttpStatus.OK);
        customResponse.setMessage("Updated");

        when(competencyAreaService.updateCompArea(any(JsonNode.class))).thenReturn(customResponse);
        ResponseEntity<CustomResponse> response = controller.update(node);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Updated", response.getBody().getMessage());
    }

    @Test
    void testSearchCompetencyArea() {
        SearchCriteria criteria = new SearchCriteria();
        CustomResponse customResponse = new CustomResponse();
        customResponse.setResponseCode(HttpStatus.OK);
        customResponse.setMessage("Search successful");

        when(competencyAreaService.searchCompArea(criteria)).thenReturn(customResponse);
        ResponseEntity<?> response = controller.search(criteria);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Search successful", ((CustomResponse) response.getBody()).getMessage());
    }

    @Test
    void testReadCompetencyArea() {
        CustomResponse customResponse = new CustomResponse();
        customResponse.setResponseCode(HttpStatus.OK);
        customResponse.setMessage("Read success");

        when(competencyAreaService.readCompArea("123")).thenReturn(customResponse);
        ResponseEntity<?> response = controller.competencyAreatRead("123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Read success", ((CustomResponse) response.getBody()).getMessage());
    }

    @Test
    void testDeleteCompetencyArea() {
        CustomResponse customResponse = new CustomResponse();
        customResponse.setResponseCode(HttpStatus.OK);
        customResponse.setMessage("Deleted");

        when(competencyAreaService.deleteCompetencyArea("123")).thenReturn(customResponse);
        ResponseEntity<CustomResponse> response = controller.deleteCompetencyArea("123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Deleted", response.getBody().getMessage());
    }
}


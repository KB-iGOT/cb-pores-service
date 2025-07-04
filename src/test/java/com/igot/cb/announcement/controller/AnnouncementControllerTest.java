package com.igot.cb.announcement.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.announcement.service.AnnouncementService;
import com.igot.cb.pores.dto.CustomResponse;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AnnouncementControllerTest {

    @Mock
    private AnnouncementService announcementService;

    @InjectMocks
    private AnnouncementController announcementController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
    }

    @Test
    void testCreate() throws Exception {
        JsonNode input = objectMapper.readTree("{\"title\":\"Test Announcement\"}");

        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.CREATED);
        when(announcementService.createAnnouncement(input)).thenReturn(mockResponse);

        ResponseEntity<CustomResponse> response = announcementController.create(input);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(announcementService, times(1)).createAnnouncement(input);
    }

    @Test
    void testSearch() {
        SearchCriteria criteria = new SearchCriteria();

        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.OK);
        when(announcementService.searchAnnouncement(criteria)).thenReturn(mockResponse);

        ResponseEntity<?> response = announcementController.search(criteria);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(announcementService, times(1)).searchAnnouncement(criteria);
    }

    @Test
    void testAssign() throws Exception {
        JsonNode input = objectMapper.readTree("{\"id\":\"1\",\"title\":\"Updated\"}");

        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.OK);
        when(announcementService.updateAnnouncement(input)).thenReturn(mockResponse);

        ResponseEntity<CustomResponse> response = announcementController.assign(input);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(announcementService, times(1)).updateAnnouncement(input);
    }

    @Test
    void testRead() {
        String id = "123";

        CustomResponse mockResponse = new CustomResponse();
        when(announcementService.readAnnouncement(id)).thenReturn(mockResponse);

        ResponseEntity<?> response = announcementController.read(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(announcementService, times(1)).readAnnouncement(id);
    }

    @Test
    void testDelete() {
        String id = "123";

        CustomResponse mockResponse = new CustomResponse();
        when(announcementService.deleteAnnouncement(id)).thenReturn(mockResponse);

        ResponseEntity<?> response = announcementController.delete(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(announcementService, times(1)).deleteAnnouncement(id);
    }
}

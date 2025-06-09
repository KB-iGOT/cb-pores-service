package com.igot.cb.cios.controller;
import com.igot.cb.cios.dto.ObjectDto;
import com.igot.cb.cios.service.CiosContentService;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import com.igot.cb.pores.elasticsearch.dto.SearchResult;
import com.igot.cb.pores.util.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CiosControllerTest {

    @InjectMocks
    private CiosController ciosController;

    @Mock
    private CiosContentService ciosContentService;

    @Test
    void test_onboardContent_returnsCorrectResponse() {
        // Arrange
        List<ObjectDto> objectDtoList = List.of(new ObjectDto());

        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.OK);

        when(ciosContentService.onboardContent(any())).thenReturn(mockResponse);

        // Act
        ResponseEntity<Object> result = ciosController.onboardContent(objectDtoList);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockResponse, result.getBody());
    }


    @Test
    void test_searchContent_returnsCorrectResponse() {
        // Arrange
        SearchCriteria criteria = new SearchCriteria();
        SearchResult mockResult = new SearchResult();

        when(ciosContentService.searchCotent(any())).thenReturn(mockResult);

        // Act
        ResponseEntity<?> result = ciosController.searchContent(criteria);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockResult, result.getBody());
    }

    @Test
    void test_deleteContent_returnsCorrectResponse() {
        // Arrange
        String contentId = "abc123";
        Map<String, Object> mockResponse = Map.of("status", "deleted");

        when(ciosContentService.deleteContent(anyString())).thenReturn(mockResponse);

        // Act
        ResponseEntity<Object> result = ciosController.deleteContent(contentId);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockResponse, result.getBody());
    }

    @Test
    void test_fetchData_returnsCorrectResponse() {
        // Arrange
        String contentId = "xyz456";
        Map<String, Object> mockResponse = Map.of("data", "value");

        when(ciosContentService.fetchDataByContentId(anyString())).thenReturn(mockResponse);

        // Act
        ResponseEntity<Object> result = ciosController.fetchData(contentId);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockResponse, result.getBody());
    }

    @Test
    void test_fetchDataByExternalId_returnsCorrectResponse() {
        // Arrange
        String externalId = "ext001";
        String partnerId = "partnerX";
        Map<String, Object> mockResponse = Map.of("external", "content");

        when(ciosContentService.fetchDataByExternalIdAndPartnerId(anyString(), anyString())).thenReturn(mockResponse);

        // Act
        ResponseEntity<Object> result = ciosController.fetchDataByExternalId(externalId, partnerId);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockResponse, result.getBody());
    }
}

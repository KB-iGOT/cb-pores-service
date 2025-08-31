package com.igot.cb.orgbookmark.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.orgbookmark.service.OrgBookmarkService;
import com.igot.cb.pores.dto.CustomResponse;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import com.igot.cb.pores.util.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrgBookmarkControllerTest {

    @InjectMocks
    private OrgBookmarkController orgBookmarkController;

    @Mock
    private OrgBookmarkService orgBookmarkService;

    /**
     * Test case for the search method in OrgBookmarkController.
     * It verifies that the search method returns the correct ResponseEntity
     * with the CustomResponse and HTTP status code returned by the service.
     */
    @Test
    public void testSearchReturnsCorrectResponseEntity() {
        // Arrange
        SearchCriteria searchCriteria = new SearchCriteria();
        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.OK);
        when(orgBookmarkService.search(searchCriteria)).thenReturn(mockResponse);

        // Act
        ResponseEntity<?> result = orgBookmarkController.search(searchCriteria);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockResponse, result.getBody());
    }

    /**
     * Tests the search method with a null SearchCriteria object.
     * This test verifies that the method handles a null input gracefully.
     */
    @Test
    public void testSearchWithNullSearchCriteria() {
        SearchCriteria nullSearchCriteria = null;
        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.BAD_REQUEST);

        when(orgBookmarkService.search(nullSearchCriteria)).thenReturn(mockResponse);

        ResponseEntity<?> responseEntity = orgBookmarkController.search(nullSearchCriteria);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    /**
     * Test case for createOrgBookmark method when a valid organization bookmark is created.
     * It verifies that the method returns the correct ResponseEntity with the API response
     * and status code from the service layer.
     */
    @Test
    public void test_createOrgBookmark_validInput() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode orgDetails = objectMapper.readTree("{\"name\":\"Test Org\",\"id\":\"123\"}");
        String userAuthToken = "valid-auth-token";

        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.CREATED);

        when(orgBookmarkService.createOrgBookmark(orgDetails, userAuthToken)).thenReturn(mockResponse);

        ResponseEntity<?> responseEntity = orgBookmarkController.createOrgBookmark(orgDetails, userAuthToken);

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertEquals(mockResponse, responseEntity.getBody());
    }

    /**
     * Test the deleteOrgBookmarkById method when the service returns a not found response.
     * This test verifies that the controller correctly handles and returns the not found response from the service.
     */
    @Test
    public void test_deleteOrgBookmarkById_notFound() {
        String id = "nonexistentId";
        String token = "validToken";
        ApiResponse notFoundResponse = new ApiResponse();
        notFoundResponse.setResponseCode(HttpStatus.NOT_FOUND);
        when(orgBookmarkService.deleteOrgBookmarkById(id)).thenReturn(notFoundResponse);

        ResponseEntity<?> responseEntity = orgBookmarkController.deleteOrgBookmarkById(id, token);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    /**
     * Test case for deleteOrgBookmarkById method
     * Verifies that the method returns the correct ResponseEntity with the ApiResponse and status code
     */
    @Test
    public void test_deleteOrgBookmarkById_returnsCorrectResponseEntity() {
        String id = "testId";
        String token = "testToken";
        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.OK);

        when(orgBookmarkService.deleteOrgBookmarkById(id)).thenReturn(mockResponse);

        ResponseEntity<?> result = orgBookmarkController.deleteOrgBookmarkById(id, token);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockResponse, result.getBody());
    }

    /**
     * Test the readOrgBookmarkById method with an empty ID.
     * This test verifies that when an empty string is passed as the ID,
     * the method returns an appropriate error response.
     */
    @Test
    public void test_readOrgBookmarkById_emptyId() {
        String emptyId = "";
        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.BAD_REQUEST);

        when(orgBookmarkService.readOrgBookmarkById(emptyId)).thenReturn(mockResponse);

        ResponseEntity<?> responseEntity = orgBookmarkController.readOrgBookmarkById(emptyId);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(mockResponse, responseEntity.getBody());
    }

    /**
     * Tests the readOrgBookmarkById method of OrgBookmarkController.
     * Verifies that the method correctly delegates to the service layer and returns the expected ResponseEntity.
     */
    @Test
    public void test_readOrgBookmarkById_returnsCorrectResponseEntity() {
        // Arrange

        String id = "testId";
        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.OK);
        when(orgBookmarkService.readOrgBookmarkById(id)).thenReturn(mockResponse);

        // Act
        ResponseEntity<?> result = orgBookmarkController.readOrgBookmarkById(id);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockResponse, result.getBody());
    }

    /**
     * Test case for updating an organization bookmark.
     * This test verifies that the updateOrgBookmark method correctly processes
     * the request and returns the expected response.
     */
    @Test
    public void test_updateOrgBookmark_success() {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode orgDetails = objectMapper.createObjectNode().put("id", "123").put("name", "Test Org");
        String userAuthToken = "test-auth-token";

        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.OK);

        when(orgBookmarkService.updateOrgBookmark(orgDetails, userAuthToken)).thenReturn(mockResponse);

        ResponseEntity<?> responseEntity = orgBookmarkController.updateOrgBookmark(orgDetails, userAuthToken);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(mockResponse, responseEntity.getBody());
    }

}

package com.igot.cb.designation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.designation.service.DesignationService;
import com.igot.cb.pores.dto.CustomResponse;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import com.igot.cb.pores.util.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class DesignationControllerTest {

    @InjectMocks
    private DesignationController designationController;

    @Mock
    private DesignationService designationService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testCreateDesignationWithEmptyJsonNode() {
        /**
         * Test creating a designation with an empty JsonNode.
         * This test verifies that the controller handles an empty input correctly.
         */
        ObjectMapper mapper = new ObjectMapper();
        JsonNode emptyNode = mapper.createObjectNode();

        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.BAD_REQUEST);

        when(designationService.createDesignation(emptyNode)).thenReturn(mockResponse);

        ResponseEntity<CustomResponse> responseEntity = designationController.createDesignation(emptyNode);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    /**
     * Test the createTerm method with an empty JsonNode request.
     * This test verifies that the method handles an empty request appropriately.
     */
    @Test
    public void testCreateTermWithEmptyRequest() {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode emptyRequest = mapper.createObjectNode();

        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.BAD_REQUEST);

        when(designationService.createTerm(emptyRequest)).thenReturn(mockResponse);

        ResponseEntity<ApiResponse> responseEntity = designationController.createTerm(emptyRequest);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    /**
     * Tests the deleteDesignation method with an empty ID.
     * This test verifies that when an empty ID is provided, the method
     * returns an appropriate error response.
     */
    @Test
    public void testDeleteDesignationWithEmptyId() {
        String emptyId = "";
        CustomResponse errorResponse = new CustomResponse();
        errorResponse.setResponseCode(HttpStatus.BAD_REQUEST);
        errorResponse.setMessage("Invalid designation ID");

        when(designationService.deleteDesignation(emptyId)).thenReturn(errorResponse);

        ResponseEntity<CustomResponse> response = designationController.deleteDesignation(emptyId);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid designation ID", response.getBody().getMessage());
    }

    /**
     * Tests the playListRead method of DesignationController.
     * This test verifies that the method correctly returns a ResponseEntity
     * with the CustomResponse and appropriate HTTP status code when given a valid ID.
     */
    @Test
    public void testPlayListReadWithValidId() {
        String id = "1";
        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.OK);

        when(designationService.readDesignation(id)).thenReturn(mockResponse);

        ResponseEntity<?> result = designationController.playListRead(id);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockResponse, result.getBody());
    }

    /**
     * Tests the search method of DesignationController.
     * Verifies that the method correctly delegates to the designationService
     * and returns the appropriate ResponseEntity.
     */
    @Test
    public void testSearchDesignation() {
        // Arrange
        SearchCriteria searchCriteria = new SearchCriteria();
        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.OK);
        when(designationService.searchDesignation(searchCriteria)).thenReturn(mockResponse);

        // Act
        ResponseEntity<?> result = designationController.search(searchCriteria);

        // Assert
        verify(designationService).searchDesignation(searchCriteria);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockResponse, result.getBody());
    }

    /**
     * Tests the search method with a null SearchCriteria object.
     * Verifies that the method handles the null input by returning an appropriate error response.
     */
    @Test
    public void testSearchWithNullSearchCriteria() {
        SearchCriteria nullSearchCriteria = null;
        CustomResponse errorResponse = new CustomResponse();
        errorResponse.setResponseCode(HttpStatus.BAD_REQUEST);

        when(designationService.searchDesignation(nullSearchCriteria)).thenReturn(errorResponse);

        ResponseEntity<?> responseEntity = designationController.search(nullSearchCriteria);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(errorResponse, responseEntity.getBody());
    }

    /**
     * Test the update method of DesignationController
     * Verifies that the controller returns the correct ResponseEntity
     * with the CustomResponse and status code from the service
     */
    @Test
    public void testUpdateDesignation() {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode updateDesignationDetails = objectMapper.createObjectNode();

        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.OK);

        when(designationService.updateDesignation(updateDesignationDetails)).thenReturn(mockResponse);

        ResponseEntity<CustomResponse> responseEntity = designationController.update(updateDesignationDetails);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(mockResponse, responseEntity.getBody());
    }

    /**
     * Test case for createDesignation method when a valid designation is created.
     * It verifies that the method returns the correct ResponseEntity with the expected CustomResponse.
     */
    @Test
    public void test_createDesignation_validDesignation() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode designationDetails = objectMapper.readTree("{\"name\":\"Test Designation\"}");

        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.CREATED);

        when(designationService.createDesignation(designationDetails)).thenReturn(mockResponse);

        ResponseEntity<CustomResponse> responseEntity = designationController.createDesignation(designationDetails);

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertEquals(mockResponse, responseEntity.getBody());
    }

    /**
     * Test case for createTerm method in DesignationController
     * Verifies that the method returns the correct ResponseEntity with ApiResponse
     */
    @Test
    public void test_createTerm_returnsCorrectResponseEntity() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode request = objectMapper.readTree("{\"key\": \"value\"}");

        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.CREATED);

        when(designationService.createTerm(request)).thenReturn(mockResponse);

        ResponseEntity<ApiResponse> result = designationController.createTerm(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(mockResponse, result.getBody());
    }

    /**
     * Test case for deleteDesignation method when a valid designation ID is provided.
     * It verifies that the method calls the service layer and returns the correct response.
     */
    @Test
    public void test_deleteDesignation_validId() {
        String id = "123";
        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.OK);

        when(designationService.deleteDesignation(id)).thenReturn(mockResponse);

        ResponseEntity<CustomResponse> responseEntity = designationController.deleteDesignation(id);

        verify(designationService).deleteDesignation(id);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(mockResponse, responseEntity.getBody());
    }

    /**
     * Test case for successful loading of designations from excel file.
     * Verifies that the loadDesignation method returns a ResponseEntity with OK status
     * and the expected success message when the file is processed successfully.
     */
    @Test
    public void test_loadDesignation_SuccessfulUpload() {
        // Arrange

        MultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "test data".getBytes());
        String token = "testToken";

        doNothing().when(designationService).loadDesignation(file, token);

        // Act
        ResponseEntity<String> response = designationController.loadDesignation(file, token);

        // Assert
        assertEquals("Loading of designations from excel is successful.", response.getBody());
        assertEquals(200, response.getStatusCodeValue());
        verify(designationService, times(1)).loadDesignation(file, token);
    }

    /**
     * Test the loadDesignation method when an exception occurs during processing.
     * This test verifies that the method returns an appropriate error response
     * when the designationService throws an exception.
     */
    @Test
    public void test_loadDesignation_exception_handling() {
        // Arrange
        MultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "test data".getBytes());
        String token = "testToken";

        doThrow(new RuntimeException("Test exception")).when(designationService).loadDesignation(Mockito.any(MultipartFile.class), Mockito.anyString());

        // Act
        ResponseEntity<String> response = designationController.loadDesignation(file, token);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Error during loading of designation from excel: Test exception", response.getBody());
    }

}

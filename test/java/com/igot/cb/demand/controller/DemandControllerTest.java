package com.igot.cb.demand.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.demand.service.DemandService;
import com.igot.cb.pores.dto.CustomResponse;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class DemandControllerTest {

    @InjectMocks
    private DemandController demandController;

    @Mock
    private DemandService demandService;

    /**
     * Test case for the delete method in DemandController.
     * It verifies that the method correctly calls the demandService.delete() method
     * and returns the expected ResponseEntity with OK status.
     */
    @Test
    void testDeleteDemand() {
        String demandId = "123";
        String expectedResponse = "Demand deleted successfully";

        when(demandService.delete(demandId)).thenReturn(expectedResponse);

        ResponseEntity<String> responseEntity = demandController.delete(demandId);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(expectedResponse, responseEntity.getBody());
    }

    /**
     * Test case for reading a demand by ID.
     * This test verifies that the read method in DemandController
     * correctly processes the request and returns the expected response.
     */
    @Test
    void testReadDemandById() {
        String demandId = "123";
        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.OK);

        when(demandService.readDemand(demandId)).thenReturn(mockResponse);

        ResponseEntity<?> result = demandController.read(demandId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockResponse, result.getBody());
    }

    /**
     * Tests the search method of DemandController.
     * Verifies that the method returns a ResponseEntity with the correct response and status code.
     */
    @Test
    void testSearchReturnsCorrectResponse() {
        SearchCriteria searchCriteria = new SearchCriteria();
        CustomResponse expectedResponse = new CustomResponse();
        expectedResponse.setResponseCode(HttpStatus.OK);

        when(demandService.searchDemand(searchCriteria)).thenReturn(expectedResponse);

        ResponseEntity<?> result = demandController.search(searchCriteria);

        assertEquals(expectedResponse, result.getBody());
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    /**
     * Tests the search method with a null SearchCriteria.
     * This test verifies that the method handles a null input gracefully by returning an appropriate error response.
     */
    @Test
    void testSearchWithNullSearchCriteria() {
        SearchCriteria nullCriteria = null;
        CustomResponse errorResponse = new CustomResponse();
        errorResponse.setResponseCode(HttpStatus.BAD_REQUEST);

        when(demandService.searchDemand(nullCriteria)).thenReturn(errorResponse);

        ResponseEntity<?> responseEntity = demandController.search(nullCriteria);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(errorResponse, responseEntity.getBody());
    }

    /**
     * Test case for creating a demand successfully.
     * This test verifies that the create method in DemandController
     * correctly processes a valid demand creation request and returns
     * the expected CustomResponse with the appropriate HTTP status.
     */
    @Test
    void test_create_successful_demand_creation() throws Exception {
        // Prepare test data
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode demandsDetails = objectMapper.readTree("{\"demandName\":\"Test Demand\"}");
        String token = "test-token";
        String rootOrgId = "test-org-id";

        // Mock the service response
        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.CREATED);
        when(demandService.createDemand(demandsDetails, token, rootOrgId)).thenReturn(mockResponse);

        // Call the controller method
        ResponseEntity<CustomResponse> responseEntity = demandController.create(demandsDetails, token, rootOrgId);

        // Verify the results
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertEquals(mockResponse, responseEntity.getBody());
        verify(demandService, times(1)).createDemand(demandsDetails, token, rootOrgId);
    }

    /**
     * Test case for updateStatus method in DemandController
     * Verifies that the method correctly processes the request and returns the expected response
     */
    @Test
    void test_updateStatus_successful() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode updateDetails = objectMapper.readTree("{\"demandId\":\"123\",\"status\":\"Approved\"}");
        String token = "test-token";
        String rootOrgId = "test-org";

        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.OK);

        when(demandService.updateDemandStatus(updateDetails, token, rootOrgId)).thenReturn(mockResponse);

        ResponseEntity<CustomResponse> responseEntity = demandController.updateStatus(updateDetails, token, rootOrgId);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

}

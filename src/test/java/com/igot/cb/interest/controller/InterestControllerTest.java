package com.igot.cb.interest.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.interest.service.InterestService;
import com.igot.cb.pores.dto.CustomResponse;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import com.igot.cb.pores.util.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InterestControllerTest {

    @InjectMocks
    private InterestController interestController;

    @Mock
    private InterestService interestService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Test case for the assign method in InterestController.
     * It verifies that the method correctly calls the interestService and returns the expected ResponseEntity.
     */
    @Test
    public void testAssignInterestToDemand() throws Exception {
        // Prepare test data
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode interestDetails = objectMapper.readTree("{\"key\":\"value\"}");
        String token = "test-token";

        // Mock the service response
        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.OK);
        when(interestService.assignInterestToDemand(interestDetails, token)).thenReturn(mockResponse);

        // Call the method under test
        ResponseEntity<CustomResponse> result = interestController.assign(interestDetails, token);

        // Verify the result
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockResponse, result.getBody());

        // Verify that the service method was called with the correct parameters
        verify(interestService, times(1)).assignInterestToDemand(interestDetails, token);
    }

    /**
     * Tests the read method of InterestController.
     * Verifies that the method returns a ResponseEntity with HttpStatus.OK
     * and the CustomResponse object returned by the InterestService.
     */
    @Test
    public void testReadReturnsResponseEntityWithCustomResponse() {
        // Arrange
        String id = "testId";
        CustomResponse expectedResponse = new CustomResponse();
        when(interestService.read(id)).thenReturn(expectedResponse);

        // Act
        ResponseEntity<?> result = interestController.read(id);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(expectedResponse, result.getBody());
    }

    /**
     * Tests the read method with an empty ID.
     * This test verifies that the method handles an empty ID input correctly,
     * assuming that the service method would return an appropriate error response.
     */
    @Test
    public void testReadWithEmptyId() {
        String emptyId = "";
        CustomResponse expectedResponse = new CustomResponse();
        expectedResponse.setResponseCode(HttpStatus.BAD_REQUEST);

        when(interestService.read(emptyId)).thenReturn(expectedResponse);

        ResponseEntity<?> result = interestController.read(emptyId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    /**
     * Tests the search method with null SearchCriteria.
     * This test verifies that when a null SearchCriteria is provided,
     * the method handles it gracefully and returns an appropriate error response.
     */
    @Test
    public void testSearchWithNullSearchCriteria() {
        SearchCriteria nullSearchCriteria = null;
        CustomResponse expectedResponse = new CustomResponse();
        expectedResponse.setResponseCode(HttpStatus.BAD_REQUEST);
        expectedResponse.setMessage("Invalid search criteria");

        when(interestService.searchDemand(nullSearchCriteria)).thenReturn(expectedResponse);

        ResponseEntity<?> responseEntity = interestController.search(nullSearchCriteria);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals(expectedResponse, responseEntity.getBody());
    }

    /**
     * Tests the assign method with a null auth token.
     * Verifies that the method handles null token by returning an appropriate error response.
     */
    @Test
    public void test_assign_with_null_auth_token() {
        JsonNode interestDetails = mock(JsonNode.class);
        CustomResponse expectedResponse = new CustomResponse();
        expectedResponse.setResponseCode(HttpStatus.UNAUTHORIZED);

        when(interestService.assignInterestToDemand(interestDetails, null)).thenReturn(expectedResponse);

        ResponseEntity<CustomResponse> result = interestController.assign(interestDetails, null);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        assertEquals(expectedResponse, result.getBody());
    }

    /**
     * Tests the assign method with a null interestDetails.
     * Verifies that the method handles null input by returning an appropriate error response.
     */
    @Test
    public void test_assign_with_null_interest_details() {
        String token = "validToken";
        CustomResponse expectedResponse = new CustomResponse();
        expectedResponse.setResponseCode(HttpStatus.BAD_REQUEST);

        when(interestService.assignInterestToDemand(null, token)).thenReturn(expectedResponse);

        ResponseEntity<CustomResponse> result = interestController.assign(null, token);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals(expectedResponse, result.getBody());
    }

    /**
     * Test case for creating an interest successfully.
     * This test verifies that the create method in InterestController
     * correctly processes a valid interest creation request and returns
     * the expected response.
     */
    @Test
    public void test_create_successful() {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode interestDetails = objectMapper.createObjectNode().put("interestName", "Test Interest");

        CustomResponse mockResponse = new CustomResponse();
        mockResponse.setResponseCode(HttpStatus.CREATED);

        when(interestService.createInterest(interestDetails)).thenReturn(mockResponse);

        ResponseEntity<CustomResponse> responseEntity = interestController.create(interestDetails);

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertEquals(mockResponse, responseEntity.getBody());
    }

}

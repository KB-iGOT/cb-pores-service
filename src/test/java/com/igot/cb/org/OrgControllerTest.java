package com.igot.cb.org;

import com.igot.cb.org.service.OrgService;
import com.igot.cb.pores.util.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrgControllerTest {

    @InjectMocks
    private OrgController orgController;

    @Mock
    private OrgService orgService;

    /**
     * Test the readFramework method of OrgController
     * Verifies that the method correctly returns a ResponseEntity with the ApiResponse
     * and status code returned by the OrgService
     */
    @Test
    void test_readFramework_returnsCorrectResponse() {
        // Arrange
        String frameworkName = "testFramework";
        String orgId = "testOrgId";
        String termName = "testTerm";
        String userAuthToken = "testAuthToken";

        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setResponseCode(HttpStatus.OK);

        when(orgService.readFramework(frameworkName, orgId, termName, userAuthToken)).thenReturn(mockResponse);

        // Act
        ResponseEntity<Object> result = orgController.readFramework(frameworkName, orgId, termName, userAuthToken);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockResponse, result.getBody());
    }

}

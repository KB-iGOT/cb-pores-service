package com.igot.cb.demandinterest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.pores.exceptions.CustomException;
import com.igot.cb.pores.util.Constants;
import com.igot.cb.transactional.cassandrautils.CassandraOperation;
import java.util.*;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KarmaQuestServiceImplTest {

    @Mock
    private CassandraOperation cassandraOperation;

    @InjectMocks
    private KarmaQuestServiceImpl karmaQuestService;

    @Mock
    private ObjectMapper objectMapper;

    private static final String INTEREST_TOPIC = "test-interest-topic";

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    /**
     * Test case for getInterest method
     * Verifies that the method correctly retrieves and processes interest data from the database
     */
    @Test
    void test_getInterest_retrievesAndProcessesData() {
        // Arrange
        String interestId = "test-interest-id";
        List<Map<String, Object>> mockResponse = new ArrayList<>();
        Map<String, Object> record = new HashMap<>();
        record.put(Constants.DATA, "{\"key\":\"value\"}");
        record.put(Constants.DEMAND_ID, "\"demand-id\"");
        record.put(Constants.USER_ID, "\"user-id\"");
        mockResponse.add(record);

        when(cassandraOperation.getRecordsByPropertiesByKey(eq(Constants.DATABASE), eq(Constants.TABLE), any(), any(), eq(interestId)))
            .thenReturn(mockResponse);

        // Act
        Object result = karmaQuestService.getInterest(interestId);

        // Assert
        assertNotNull(result);
        Assertions.assertInstanceOf(List.class, result);
        List<?> resultList = (List<?>) result;
        assertEquals(1, resultList.size());
        Map<String, Object> resultRecord = (Map<String, Object>) resultList.get(0);

        verify(cassandraOperation).getRecordsByPropertiesByKey(eq(Constants.DATABASE), eq(Constants.TABLE), any(), any(), eq(interestId));
    }

    @Test
    void test_insertInterest_success() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode mockRequestBody = mapper.createObjectNode();
        mockRequestBody.put(Constants.USER_ID_RQST, "user-123");
        mockRequestBody.put(Constants.INTEREST_FLAG_RQST, true);
        mockRequestBody.put(Constants.DEMAND_ID_RQST, "demand-456");

        String mockJsonString = mapper.writeValueAsString(mockRequestBody);
        when(objectMapper.writeValueAsString(mockRequestBody)).thenReturn(mockJsonString);

        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("status", "success");

        when(cassandraOperation.insertRecord(any(), any(), anyMap())).thenReturn(expectedResponse);

        Object result = karmaQuestService.insertInterest(mockRequestBody);

        assertNotNull(result);
        assertEquals(expectedResponse, result);
    }


    @Test
    void test_insertInterest_throwsCustomException_onJsonProcessingError() throws Exception {
        // Given
        ObjectNode mockRequestBody = new ObjectMapper().createObjectNode();
        mockRequestBody.put(Constants.USER_ID_RQST, "user-123");
        mockRequestBody.put(Constants.INTEREST_FLAG_RQST, true);
        mockRequestBody.put(Constants.DEMAND_ID_RQST, "demand-456");

        // Mock objectMapper to throw exception
        when(objectMapper.writeValueAsString(mockRequestBody))
                .thenThrow(new JsonProcessingException("Simulated failure") {});

        // When + Then
        CustomException ex = assertThrows(CustomException.class, () ->
                karmaQuestService.insertInterest(mockRequestBody)
        );

        assertEquals("Exception while mapping JsonNode", ex.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getHttpStatusCode());
    }

}

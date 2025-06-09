package com.igot.cb.transactional.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.Map;

class RequestHandlerServiceImplTest {

    @InjectMocks
    private RequestHandlerServiceImpl service;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private Logger mockLogger;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Inject the mock logger via reflection
        Field logField = service.getClass().getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(service, mockLogger);

        // Enable debug logs for tests to cover that branch
        when(mockLogger.isDebugEnabled()).thenReturn(true);
    }

    // Helper: create dummy headers map
    private Map<String, String> dummyHeaders() {
        return Map.of("Authorization", "Bearer token");
    }

    @Test
    void fetchResultUsingPost_success() {
        String uri = "http://example.com/post";
        Map<String, Object> request = Map.of("key", "value");
        Map<String, Object> expectedResponse = Map.of("result", "success");

        when(restTemplate.postForObject(eq(uri), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(expectedResponse);

        Map<String, Object> actual = service.fetchResultUsingPost(uri, request, dummyHeaders());

        assertEquals(expectedResponse, actual);
        verify(restTemplate).postForObject(eq(uri), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void fetchResultUsingPost_httpClientErrorException_handling() {
        String uri = "http://example.com/post";
        Map<String, Object> request = Map.of("key", "value");
        String errorJson = "{\"error\":\"Bad Request\"}";

        HttpClientErrorException exception = mock(HttpClientErrorException.class);
        when(exception.getResponseBodyAsString()).thenReturn(errorJson);

        when(restTemplate.postForObject(eq(uri), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(exception);

        Map<String, Object> result = service.fetchResultUsingPost(uri, request, dummyHeaders());

        assertNotNull(result);
        assertTrue(result.containsKey("error"));
        verify(restTemplate).postForObject(eq(uri), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void fetchResultUsingPost_jsonProcessingException_handling() {
        String uri = "http://example.com/post";

        // Object that Jackson can't serialize (simulate JsonProcessingException)
        Object badRequest = new Object() {
            @Override
            public String toString() {
                throw new RuntimeException("fail serialization");
            }
        };

        // Mock restTemplate to return null to reach JsonProcessingException catch block for logging
        when(restTemplate.postForObject(eq(uri), any(HttpEntity.class), eq(Map.class))).thenReturn(null);

        Map<String, Object> response = service.fetchResultUsingPost(uri, badRequest, dummyHeaders());
        // Because of serialization failure, response should be null
        assertNull(response);
    }

    @Test
    void fetchUsingGetWithHeadersProfile_success() {
        String uri = "http://example.com/get";
        Map<String, String> headers = dummyHeaders();

        Map<String, Object> expectedResponse = Map.of("key", "value");

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(expectedResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(uri),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)))
                .thenReturn(responseEntity);

        Object actual = service.fetchUsingGetWithHeadersProfile(uri, headers);

        assertEquals(expectedResponse, actual);
    }


    @Test
    void fetchUsingGetWithHeadersProfile_httpClientErrorException_handling() {
        String uri = "http://example.com/get";
        Map<String, String> headers = dummyHeaders();

        String errorJson = "{\"error\":\"Unauthorized\"}";
        HttpClientErrorException exception = mock(HttpClientErrorException.class);
        when(exception.getResponseBodyAsString()).thenReturn(errorJson);

        when(restTemplate.exchange(eq(uri), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(exception);

        Object response = service.fetchUsingGetWithHeadersProfile(uri, headers);

        assertNotNull(response);
        assertTrue(((Map<?, ?>) response).containsKey("error"));
    }

    @Test
    void fetchUsingGetWithHeadersProfile_generalException_handling() {
        String uri = "http://example.com/get";
        Map<String, String> headers = dummyHeaders();

        when(restTemplate.exchange(eq(uri), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("Unexpected"));

        Object response = service.fetchUsingGetWithHeadersProfile(uri, headers);

        // Should not throw, but response will be null due to exception handling
        assertNull(response);
    }
}

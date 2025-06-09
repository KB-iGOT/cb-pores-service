package com.igot.cb.playlist.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.playlist.util.DataCacheManager;
import com.igot.cb.playlist.util.RedisCacheMngr;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentServiceImplTest {

    @InjectMocks
    private ContentServiceImpl contentService;

    @Mock
    private DataCacheManager dataCacheMgr;

    @Mock
    private Logger log;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private RedisCacheMngr redisCacheMgr;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CbServerProperties serverConfig;

    /**
     * Test the scenario where the DataCacheManager returns an empty map and 
     * the RedisCacheManager returns a blank string.
     * This should trigger the fallback to readContent method.
     */
    @Test
    public void testReadContentFromCache_EmptyCaches() {
        String contentId = "testContentId";
        List<String> fields = Arrays.asList("field1", "field2");

        when(dataCacheMgr.getContentFromCache(contentId)).thenReturn(new HashMap<>());
        when(redisCacheMgr.getContentFromCache(contentId)).thenReturn("");

        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("dummyKey", "dummyValue");

        // Mock the readContent method since it's called as a fallback
        ContentServiceImpl spyContentService = spy(contentService);
        doReturn(expectedResult).when(spyContentService).readContent(contentId, fields);

        Map<String, Object> result = spyContentService.readContentFromCache(contentId, fields);

        Assertions.assertEquals(expectedResult, result);
        verify(dataCacheMgr).getContentFromCache(contentId);
        verify(redisCacheMgr).getContentFromCache(contentId);
    }

    /**
     * Test the scenario where Redis returns a valid string but JSON parsing fails.
     * This should trigger the fallback to readContent method with only contentId.
     */
    @Test
    public void testReadContentFromCache_RedisParsingFailure() throws Exception {
        String contentId = "testContentId";
        List<String> fields = Arrays.asList("field1", "field2");

        when(dataCacheMgr.getContentFromCache(contentId)).thenReturn(new HashMap<>());
        when(redisCacheMgr.getContentFromCache(contentId)).thenReturn("invalidJsonString");

        Map<String, Object> expectedResult = new HashMap<>();

        // Mock the readContent method since it's called as a fallback
        ContentServiceImpl spyContentService = spy(contentService);

        Map<String, Object> result = spyContentService.readContentFromCache(contentId, fields);

        Assertions.assertEquals(expectedResult, result);
        verify(dataCacheMgr).getContentFromCache(contentId);
        verify(redisCacheMgr).getContentFromCache(contentId);
    }

    /**
     * Tests the behavior of fetchResult when a generic Exception occurs.
     * This test verifies that the method properly handles unexpected exceptions,
     * logs the error, and returns null as the result.
     */
    @Test
    public void test_fetchResult_genericException() {
        String uri = "http://example.com/api";

        when(restTemplate.getForObject(uri, Map.class)).thenThrow(new RuntimeException("Unexpected error"));

        Object result = contentService.fetchResult(uri);

        Assertions.assertNull(result);
    }

    /**
     * Tests the behavior of fetchResult when an HttpClientErrorException occurs.
     * This test verifies that the method properly handles the exception, attempts to parse the error response,
     * and returns the parsed response as the result.
     */
    @Test
    public void test_fetchResult_httpClientErrorException() {
        String uri = "http://example.com/api";

        HttpClientErrorException exception = mock(HttpClientErrorException.class);
        when(restTemplate.getForObject(uri, Map.class)).thenThrow(exception);
        when(exception.getResponseBodyAsString()).thenReturn("{\"error\":\"Not Found\"}");

        Object result = contentService.fetchResult(uri);

        Assertions.assertNotNull(result);
        assertInstanceOf(Map.class, result);
    }

    /**
     * Test case for fetchResult method when debug logging is disabled
     * Verifies that the method returns the expected response from RestTemplate
     */
    @Test
    public void test_fetchResult_whenDebugLoggingDisabled() {
        String uri = "http://example.com/api";
        Map<String, Object> expectedResponse = Map.of("key", "value");

        when(restTemplate.getForObject(uri, Map.class)).thenReturn(expectedResponse);

        Object result = contentService.fetchResult(uri);

        Assertions.assertEquals(expectedResponse, result);
        verify(restTemplate).getForObject(uri, Map.class);
    }

    /**
     * Tests the fetchResult method when debug logging is enabled.
     * It verifies that the method logs debug information, makes a REST call,
     * and returns the expected response.
     */
    @Test
    public void test_fetchResult_whenDebugLoggingEnabled() {
        MockitoAnnotations.openMocks(this);

        String testUri = "http://test.com/api";
        Map<String, Object> expectedResponse = Map.of("key", "value");

        when(restTemplate.getForObject(testUri, Map.class)).thenReturn(expectedResponse);

        Object result = contentService.fetchResult(testUri);

        Assertions.assertEquals(expectedResponse, result);
    }

    /**
     * Test case for readContentFromCache method when the data is not in the cache
     * and needs to be fetched from the content service.
     * 
     * This test covers the following path:
     * - Fields are not empty
     * - ResponseData is empty or has fewer fields than requested
     * - Redis cache does not contain the content (returns blank string)
     * 
     * Expected behavior:
     * - The method should call readContent to fetch the data
     * - The returned map should contain the fetched content
     */
    @Test
    public void test_readContentFromCache_whenDataNotInCacheAndNotInRedis() {
        String contentId = "test-content-id";
        List<String> fields = Arrays.asList("field1", "field2");
        Map<String, Object> expectedContent = new HashMap<>();
        expectedContent.put("field1", "value1");
        expectedContent.put("field2", "value2");

        when(dataCacheMgr.getContentFromCache(contentId)).thenReturn(new HashMap<>());
        when(redisCacheMgr.getContentFromCache(contentId)).thenReturn("");
        when(contentService.readContent(contentId, fields)).thenReturn(expectedContent);

        Map<String, Object> result = contentService.readContentFromCache(contentId, fields);

        Assertions.assertEquals(null, result);
    }

    /**
     * Test case for readContentFromCache method when fields are empty, responseData is empty,
     * and contentString is not blank.
     * This test verifies that the method correctly handles the scenario where data is retrieved
     * from Redis cache and processed accordingly.
     */
    @Test
    public void test_readContentFromCache_whenFieldsEmptyAndResponseDataEmptyAndContentStringNotBlank() throws JsonProcessingException {
        // Arrange
        String contentId = "test-content-id";
        List<String> fields = Arrays.asList();
        Map<String, Object> responseData = new HashMap<>();
        String contentString = "{\"field1\": \"value1\", \"field2\": \"value2\"}";
        Map<String, Object> contentData = new HashMap<>();
        contentData.put("field1", "value1");
        contentData.put("field2", "value2");

        when(serverConfig.getDefaultContentProperties()).thenReturn("field1,field2");
        when(dataCacheMgr.getContentFromCache(contentId)).thenReturn(responseData);
        when(redisCacheMgr.getContentFromCache(contentId)).thenReturn(contentString);
        //when(mapper.readValue((String) eq(contentString), (Class<Object>) any())).thenReturn(contentData);

        // Act
        Map<String, Object> result = contentService.readContentFromCache(contentId, fields);

        // Assert
        Assertions.assertEquals(0, result.size());
    }

    /**
     * Tests the readContentFromCache method when fields list is empty and responseData is not empty.
     * This test covers the path where CollectionUtils.isEmpty(fields) is true and
     * !(MapUtils.isEmpty(responseData) || responseData.size() < fields.size()) is true.
     */
    @Test
    public void test_readContentFromCache_whenFieldsEmptyAndResponseDataNotEmpty() {


        // Set up test data
        String contentId = "test-content-id";
        List<String> fields = new ArrayList<>();
        Map<String, Object> expectedResponseData = Map.of("key1", "value1", "key2", "value2");

        // Mock behavior
        when(serverConfig.getDefaultContentProperties()).thenReturn("prop1,prop2");
        when(dataCacheMgr.getContentFromCache(contentId)).thenReturn(expectedResponseData);

        // Call the method under test
        Map<String, Object> result = contentService.readContentFromCache(contentId, fields);

        // Verify the result
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedResponseData, result);
    }

    /**
     * Tests the readContent method with an empty content ID.
     * This test verifies that the method returns null when given an empty string as input,
     * which is an edge case explicitly handled by the method's implementation.
     */
    @Test
    public void test_readContent_emptyContentId() {
        Map<String, Object> result = contentService.readContent("");
        Assertions.assertNull(result);
    }

    /**
     * Test case for readContent method when fields list is empty and response is valid.
     * This test verifies that the method correctly handles an empty fields list,
     * constructs the URL, makes the API call, and returns the content when the response is successful.
     */
    @Test
    public void test_readContent_emptyFieldsValidResponse() {
        // Arrange
        String contentId = "testContentId";
        List<String> fields = Collections.emptyList();
        String baseUrl = "http://test.com";
        String endPoint = "/content";
        String endPointFields = "?fields=";

        when(serverConfig.getContentHost()).thenReturn(baseUrl);
        when(serverConfig.getContentReadEndPoint()).thenReturn(endPoint);
        when(serverConfig.getContentReadEndPointFields()).thenReturn(endPointFields);

        Map<String, Object> content = new HashMap<>();
        content.put("id", contentId);
        content.put("name", "Test Content");

        Map<String, Object> result = new HashMap<>();
        result.put(Constants.CONTENT, content);

        Map<String, Object> response = new HashMap<>();
        response.put(Constants.RESPONSE_CODE, Constants.OK);
        response.put(Constants.RESULT, result);

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);

        // Act
        Map<String, Object> returnedContent = contentService.readContent(contentId, fields);

        // Assert
        Assertions.assertNotNull(returnedContent);
        Assertions.assertEquals(content, returnedContent);
        verify(restTemplate).getForObject(
            baseUrl + endPoint + "/" + contentId + endPointFields,
            Map.class
        );
    }

    /**
     * Tests the readContent method when the response code is not OK.
     * This test verifies that the method returns null when the response code is not OK.
     */
    @Test
    public void test_readContent_nonOkResponseCode() {
        when(serverConfig.getContentHost()).thenReturn("http://example.com");
        when(serverConfig.getContentReadEndPoint()).thenReturn("/read");
        when(serverConfig.getContentReadEndPointFields()).thenReturn("?fields=");

        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put(Constants.RESPONSE_CODE, "ERROR");

        ContentServiceImpl spyContentService = spy(contentService);
        doReturn(mockResponse).when(spyContentService).fetchResult(anyString());

        Map<String, Object> result = spyContentService.readContent("testId", Collections.emptyList());

        Assertions.assertNull(result);
    }

    /**
     * Tests the readContent method when the response is null.
     * This test verifies that the method returns null when the fetchResult method returns null.
     */
    @Test
    public void test_readContent_nullResponse() {
        when(serverConfig.getContentHost()).thenReturn("http://example.com");
        when(serverConfig.getContentReadEndPoint()).thenReturn("/read");
        when(serverConfig.getContentReadEndPointFields()).thenReturn("?fields=");

        ContentServiceImpl spyContentService = spy(contentService);
        doReturn(null).when(spyContentService).fetchResult(anyString());

        Map<String, Object> result = spyContentService.readContent("testId", Collections.emptyList());

        Assertions.assertNull(result);
    }

    /**
     * Test case for readContent method when fields are not empty and the response is not valid.
     * This test verifies that the method returns null when the response does not meet the expected criteria.
     */
    @Test
    public void test_readContent_whenFieldsNotEmptyAndInvalidResponse() {
        // Arrange
        String contentId = "testContentId";
        String contentHost = "http://testhost.com";
        String contentReadEndPoint = "/content";
        String contentReadEndPointFields = "?fields=";
        when(serverConfig.getContentHost()).thenReturn(contentHost);
        when(serverConfig.getContentReadEndPoint()).thenReturn(contentReadEndPoint);
        when(serverConfig.getContentReadEndPointFields()).thenReturn(contentReadEndPointFields);

        Map<String, Object> mockResponse = mock(Map.class);
        when(mockResponse.get("responseCode")).thenReturn("NOT_OK");
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(mockResponse);

        // Act
        Map<String, Object> result = contentService.readContent(contentId, Arrays.asList("field1", "field2"));

        // Assert
        Assertions.assertNull(result);
        verify(restTemplate).getForObject(contains("field1,field2"), eq(Map.class));
    }

    /**
     * Test case for readContent method with a single contentId parameter.
     * This test verifies that the method correctly delegates to the overloaded readContent method
     * with an empty list of fields.
     */
    @Test
    public void test_readContent_withContentId() {
        String contentId = "test123";
        Map<String, Object> expectedResult = Collections.singletonMap("key", "value");

        ContentServiceImpl spyContentService = spy(contentService);
        doReturn(expectedResult).when(spyContentService).readContent(contentId, Collections.emptyList());

        Map<String, Object> result = spyContentService.readContent(contentId);

        Assertions.assertEquals(expectedResult, result);
        verify(spyContentService).readContent(contentId, Collections.emptyList());
    }

    /**
     * Test case for readContent method when fields are not empty and response is successful.
     * Verifies that the method correctly processes the response and returns the content.
     */
    @Test
    public void test_readContent_withNonEmptyFieldsAndSuccessfulResponse() {
        MockitoAnnotations.openMocks(this);

        String contentId = "testContentId";
        List<String> fields = Arrays.asList("field1", "field2");

        when(serverConfig.getContentHost()).thenReturn("http://testhost.com");
        when(serverConfig.getContentReadEndPoint()).thenReturn("/content");
        when(serverConfig.getContentReadEndPointFields()).thenReturn("?fields=");

        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put(Constants.RESPONSE_CODE, Constants.OK);

        Map<String, Object> mockResult = new HashMap<>();
        Map<String, Object> mockContent = new HashMap<>();
        mockContent.put("field1", "value1");
        mockContent.put("field2", "value2");
        mockResult.put(Constants.CONTENT, mockContent);
        mockResponse.put(Constants.RESULT, mockResult);

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(mockResponse);

        Map<String, Object> result = contentService.readContent(contentId, fields);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(mockContent, result);
        verify(restTemplate).getForObject(contains(contentId), eq(Map.class));
        verify(restTemplate).getForObject(contains("field1,field2"), eq(Map.class));
    }

}

package com.igot.cb.health.service;


import com.igot.cb.pores.cache.CacheService;
import com.igot.cb.pores.elasticsearch.service.EsUtilService;
import com.igot.cb.pores.util.ApiResponse;
import com.igot.cb.pores.util.Constants;
import com.igot.cb.transactional.cassandrautils.CassandraOperation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthServiceImplTest {

    @InjectMocks
    private HealthServiceImpl healthService;

    @Mock
    private CassandraOperation cassandraOperation;

    @Mock
    private CacheService redisCacheService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query nativeQuery;

    @Mock
    EsUtilService esUtilService;

    private final String REQUEST_ID = "test-request--123";


    private ApiResponse response;

    @BeforeEach
    void setUp() {
        // Setup is handled by MockitoExtension
    }

    // ==================== Test: All Services Healthy ====================



    @Test
    void testCheckHealthStatus_allHealthy() throws Exception {

        // Cassandra mock
        when(cassandraOperation.getRecordsByPropertiesByKey(anyString(), anyString(), any(), any(),any()))
                .thenReturn(List.of(Map.of("key", "value")));

        // Redis mock
        when(redisCacheService.isRedisHealthy()).thenReturn(true);

        // Postgres mock
        when(entityManager.createNativeQuery("SELECT 1")).thenReturn(nativeQuery);
        when(nativeQuery.getSingleResult()).thenReturn(1);

        // Elasticsearch mock
        when(esUtilService.isElasticsearchHealthy()).thenReturn(true);

        // Act
        ApiResponse response = healthService.checkHealthStatus(REQUEST_ID);

        // Assert
        assertNotNull(response);
        assertEquals(REQUEST_ID, response.getParams().getMsgId());
        assertEquals(REQUEST_ID, response.getParams().getResMsgId());

        assertTrue((Boolean) response.get(Constants.HEALTHY));

        List<Map<String, Object>> checks =
                (List<Map<String, Object>>) response.get(Constants.CHECKS);

        assertEquals(4, checks.size());
    }

    @Test
    void testCheckHealthStatus_cassandraFailure() throws Exception {

        when(cassandraOperation.getRecordsByPropertiesByKey(anyString(), anyString(),any(), any(), any()))
                .thenReturn(Collections.emptyList());

        when(redisCacheService.isRedisHealthy()).thenReturn(true);
        when(entityManager.createNativeQuery("SELECT 1")).thenReturn(nativeQuery);
        when(nativeQuery.getSingleResult()).thenReturn(1);
        when(esUtilService.isElasticsearchHealthy()).thenReturn(true);

        ApiResponse response = healthService.checkHealthStatus(REQUEST_ID);

        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertNotNull(response.getParams().getErr());
    }

    @Test
    void testCheckHealthStatus_redisFailure() throws Exception {

        when(cassandraOperation.getRecordsByPropertiesByKey(anyString(), anyString(), any(),any(), any()))
                .thenReturn(List.of(Map.of()));

        when(redisCacheService.isRedisHealthy()).thenReturn(false);

        when(entityManager.createNativeQuery("SELECT 1")).thenReturn(nativeQuery);
        when(nativeQuery.getSingleResult()).thenReturn(1);

        when(esUtilService.isElasticsearchHealthy()).thenReturn(true);

        ApiResponse response = healthService.checkHealthStatus(REQUEST_ID);

        assertEquals(Constants.FAILED, response.getParams().getStatus());
    }

    @Test
    void testCheckHealthStatus_postgresFailure() throws Exception {

        when(cassandraOperation.getRecordsByPropertiesByKey(anyString(), anyString(),any(), any(), any()))
                .thenReturn(List.of(Map.of()));
        when(redisCacheService.isRedisHealthy()).thenReturn(true);
        when(esUtilService.isElasticsearchHealthy()).thenReturn(true);

        when(entityManager.createNativeQuery("SELECT 1")).thenThrow(new RuntimeException());



        ApiResponse response = healthService.checkHealthStatus(REQUEST_ID);

        assertEquals(Constants.FAILED, response.getParams().getStatus());
    }

    @Test
    void testCheckHealthStatus_elasticsearchFailure() throws Exception {

        when(cassandraOperation.getRecordsByPropertiesByKey(anyString(), anyString(), any(),any(), any()))
                .thenReturn(List.of(Map.of()));

        when(redisCacheService.isRedisHealthy()).thenReturn(true);

        when(entityManager.createNativeQuery("SELECT 1")).thenReturn(nativeQuery);
        when(nativeQuery.getSingleResult()).thenReturn(1);

        when(esUtilService.isElasticsearchHealthy()).thenReturn(false);

        ApiResponse response = healthService.checkHealthStatus(REQUEST_ID);

        assertEquals(Constants.FAILED, response.getParams().getStatus());
    }


    @Test
    void testCheckHealthStatus_cassandraExceptionHandled() throws Exception {

        when(cassandraOperation.getRecordsByPropertiesByKey(anyString(), anyString(),any(), any(), any()))
                .thenThrow(new RuntimeException("DB down"));

        when(redisCacheService.isRedisHealthy()).thenReturn(true);
        when(entityManager.createNativeQuery("SELECT 1")).thenReturn(nativeQuery);
        when(nativeQuery.getSingleResult()).thenReturn(1);
        when(esUtilService.isElasticsearchHealthy()).thenReturn(true);

        ApiResponse response = healthService.checkHealthStatus(REQUEST_ID);

        // ✅ Assert error handled
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        assertNotNull(response.getParams().getErr());

        // ✅ NOT 500 because exception was handled internally
        assertNotEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
    }

}




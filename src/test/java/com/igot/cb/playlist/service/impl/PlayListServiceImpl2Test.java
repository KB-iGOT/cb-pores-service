package com.igot.cb.playlist.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.playlist.dto.SearchDto;
import com.igot.cb.playlist.entity.PlayListEntity;
import com.igot.cb.playlist.repository.PlayListRepository;
import com.igot.cb.playlist.service.ContentService;
import com.igot.cb.playlist.util.RedisCacheMngr;
import com.igot.cb.pores.Service.OutboundRequestHandlerServiceImpl;
import com.igot.cb.pores.cache.CacheService;
import com.igot.cb.pores.elasticsearch.dto.SearchResult;
import com.igot.cb.pores.elasticsearch.service.EsUtilService;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import com.igot.cb.pores.util.PayloadValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayListServiceImpl2Test {

    @InjectMocks
    private PlayListServiceImpl playListService;

    @Mock
    private PlayListRepository playListRepository;

    @Mock
    private RedisTemplate<String, PlayListEntity> playListEntityRedisTemplate;

    @Mock
    private RedisCacheMngr redisCacheMngr;

    @Mock
    private ContentService contentService;

    @Mock
    private CbServerProperties cbServerProperties;

    @Mock
    private PayloadValidation payloadValidation;

    @Mock
    private EsUtilService esUtilService;

    @Mock
    private RedisTemplate<String, SearchResult> redisTemplate;

    @Mock
    private CacheService cacheService;

    @Mock
    private PlayListEntity playListEntity;

    @Mock
    private JsonNode childrenNode;

    @Mock
    private JsonNode enrichedChildrenNode;

    @Mock
    private OutboundRequestHandlerServiceImpl outboundRequestHandlerService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        playListService = new PlayListServiceImpl();
        injectField("objectMapper", objectMapper);
        injectField("playListRepository", playListRepository);
        injectField("redisCacheMngr", redisCacheMngr);
        injectField("cbServerProperties", cbServerProperties);
        injectField("outboundRequestHandlerService", outboundRequestHandlerService);
        injectField("redisInsightIndex", 1);
    }

    private void injectField(String fieldName, Object value) {
        try {
            var field = PlayListServiceImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(playListService, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    void testValidatePayload_valid() {
        Map<String, Object> filters = new HashMap<>();
        filters.put(Constants.ORGANISATION, "org1");
        filters.put(Constants.REQUEST_TYPE, List.of("playlist"));
        Map<String, Object> request = new HashMap<>();
        request.put(Constants.FILTERS, filters);
        SearchDto dto = new SearchDto();
        dto.setRequest((HashMap<String, Object>) request);
        assertDoesNotThrow(() -> invokeValidatePayload(dto));
    }

    private void invokeValidatePayload(SearchDto dto) {
        try {
            var method = PlayListServiceImpl.class.getDeclaredMethod("validatePayload", SearchDto.class);
            method.setAccessible(true);
            method.invoke(playListService, dto);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testFetchContentDetails_withValidArray() throws Exception {
        ArrayNode arrayNode = objectMapper.createArrayNode().add("content-id-1");

        Map<String, Object> responseMock = Map.of("result", Map.of("content", List.of("data")));
        when(cbServerProperties.getCourseCategoryFacet()).thenReturn(new String[]{"category"});
        when(cbServerProperties.getSbSearchServiceHost()).thenReturn("http://host");
        when(cbServerProperties.getSbCompositeV4Search()).thenReturn("/search");
        when(outboundRequestHandlerService.fetchResultUsingPost(anyString(), any(), any())).thenReturn(responseMock);

        var method = PlayListServiceImpl.class.getDeclaredMethod("fetchContentDetails", JsonNode.class);
        method.setAccessible(true);
        Object result = method.invoke(playListService, arrayNode);

        assertNotNull(result);
        assertTrue(result instanceof Map);
        assertEquals(responseMock, result);
    }

    @Test
    void testPersistInRedis_success() throws Exception {
        ObjectNode enriched = objectMapper.createObjectNode();
        enriched.put("id", "org1");
        enriched.put("children", objectMapper.createArrayNode().add("c1"));

        PlayListEntity entity = new PlayListEntity();
        entity.setOrgId("org1");
        entity.setRequestType("playlist");

        var method = PlayListServiceImpl.class.getDeclaredMethod("persistInRedis", ObjectNode.class, PlayListEntity.class, String.class);
        method.setAccessible(true);
        doNothing().when(redisCacheMngr).hset(any(), anyInt(), any());

        method.invoke(playListService, enriched, entity, "org1playlist");
        verify(redisCacheMngr, times(1)).hset(any(), anyInt(), any());
    }

}


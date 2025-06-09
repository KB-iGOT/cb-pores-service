package com.igot.cb.competencies.area.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.authentication.util.AccessTokenValidator;
import com.igot.cb.competencies.area.entity.CompetencyAreaEntity;
import com.igot.cb.competencies.area.repository.CompetencyAreaRepository;
import com.igot.cb.pores.cache.CacheService;
import com.igot.cb.pores.dto.CustomResponse;
import com.igot.cb.pores.elasticsearch.dto.SearchResult;
import com.igot.cb.pores.elasticsearch.service.EsUtilService;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import com.igot.cb.pores.util.FileProcessService;
import com.igot.cb.pores.util.PayloadValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompetencyAreaServiceImpl2Test {

    @InjectMocks
    private CompetencyAreaServiceImpl competencyAreaService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PayloadValidation payloadValidation;

    @Mock
    private EsUtilService esUtilService;

    @Mock
    private CacheService cacheService;

    @Mock
    private CbServerProperties cbServerProperties;

    @Mock
    private CompetencyAreaRepository competencyAreaRepository;

    @Mock
    private AccessTokenValidator accessTokenValidator;

    @Mock
    private RedisTemplate<String, SearchResult> redisTemplate;

    @Mock
    private FileProcessService fileProcessService;

    @Mock
    private MultipartFile multipartFile;

    @Value("${search.result.redis.ttl}")
    private long searchResultRedisTtl;



    private final String TOKEN = "sample-token";

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(competencyAreaService, "objectMapper", objectMapper);
    }

    @Test
    void testCreateCompArea_Success() throws Exception {
        // Prepare input JsonNode
        ObjectMapper realMapper = new ObjectMapper();
        ObjectNode inputNode = realMapper.createObjectNode();
        inputNode.put(Constants.TITLE, "Leadership");

        // Mock payload validation
        doNothing().when(payloadValidation).validatePayload(anyString(), any());

        // Mock ES index check and search
        when(esUtilService.isIndexPresent(Constants.COMP_AREA_INDEX_NAME)).thenReturn(true);

        SearchResult mockSearchResult = new SearchResult();
        ArrayNode dataArray = realMapper.createArrayNode();  // no duplicates
        mockSearchResult.setData(dataArray);

        when(esUtilService.searchDocuments(eq(Constants.COMP_AREA_INDEX_NAME), any()))
                .thenReturn(mockSearchResult);

        // Mock token validator
        when(accessTokenValidator.verifyUserToken(TOKEN)).thenReturn("user-123");

        // Mock repository count
        when(competencyAreaRepository.count()).thenReturn(4L);

        // Mock save
        ArgumentCaptor<CompetencyAreaEntity> entityCaptor = ArgumentCaptor.forClass(CompetencyAreaEntity.class);
        when(competencyAreaRepository.save(entityCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock cache put
        doNothing().when(cacheService).putCache(anyString(), any());

        // Act
        CustomResponse response = competencyAreaService.createCompArea(inputNode, TOKEN);

        // Assert
        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertEquals(Constants.SUCCESSFULLY_CREATED, response.getMessage());
        assertNotNull(response.getResult());
        assertTrue(response.getResult().containsKey(Constants.ID));

        // Verify repository save call
        CompetencyAreaEntity savedEntity = entityCaptor.getValue();
        assertEquals("COMAREA-000005", savedEntity.getId());
    }

    @Test
    void testLoadCompetencyArea_Success() throws Exception {
        // Arrange
        String token = "valid-token";
        String userId = "user123";
        long count = 5;

        List<Map<String, String>> excelData = new ArrayList<>();
        Map<String, String> entry = new HashMap<>();
        entry.put(Constants.COMPETENCY_AREA_TYPE, "New Area");
        entry.put(Constants.DESCRIPTION, "Area Description");
        excelData.add(entry);

        // Mock token validation
        when(accessTokenValidator.verifyUserToken(token)).thenReturn(userId);

        // Mock ES index present and with existing data
        when(esUtilService.isIndexPresent(Constants.COMP_AREA_INDEX_NAME)).thenReturn(true);
        JsonNode existingData = new ObjectMapper().createArrayNode();
        SearchResult mockResult = new SearchResult();
        mockResult.setData(existingData);
        when(esUtilService.searchDocuments(eq(Constants.COMP_AREA_INDEX_NAME), any())).thenReturn(mockResult);

        // Mock repository count
        when(competencyAreaRepository.count()).thenReturn(count);

        // Mock file processing
        when(fileProcessService.processExcelFile(multipartFile)).thenReturn(excelData);

        // Avoid actual save calls
        when(competencyAreaRepository.saveAll(any())).thenReturn(null);
        when(cbServerProperties.getElasticCompJsonPath()).thenReturn("dummy-path");

        // Act
        competencyAreaService.loadCompetencyArea(multipartFile, token);

        // Assert
        verify(accessTokenValidator).verifyUserToken(token);
        verify(fileProcessService).processExcelFile(multipartFile);
        verify(competencyAreaRepository).saveAll(any());
        verify(esUtilService, atLeastOnce()).addDocument(any(), any(), any(), any(), any());
        verify(cacheService, atLeastOnce()).putCache(any(), any());
        verify(payloadValidation, atLeastOnce()).validatePayload(eq(Constants.COMP_AREA_PAYLOAD_VALIDATION), any());
    }
}

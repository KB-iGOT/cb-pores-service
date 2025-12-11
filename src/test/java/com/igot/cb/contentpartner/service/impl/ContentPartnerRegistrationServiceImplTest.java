package com.igot.cb.contentpartner.service.impl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.contentpartner.entity.ContentPartnerRegistrationEntity;
import com.igot.cb.contentpartner.repository.ContentPartnerRegistrationRepository;
import com.igot.cb.pores.cache.CacheService;
import com.igot.cb.pores.elasticsearch.service.EsUtilService;
import com.igot.cb.pores.util.ApiResponse;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import com.igot.cb.pores.util.PayloadValidation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class ContentPartnerRegistrationServiceImplTest {

    @Mock
    private PayloadValidation payloadValidation;
    @Mock
    private ContentPartnerRegistrationRepository registrationRepository;
    @Mock
    private CacheService cacheService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private CbServerProperties cbServerProperties;
    @Mock
    private EsUtilService esUtilService;

    @InjectMocks
    private ContentPartnerRegistrationServiceImpl service;

    private final ObjectMapper realMapper = new ObjectMapper();


    // CREATE TEST CASES

    @Test
    void testCreate_Success() {
        ObjectNode request = realMapper.createObjectNode();
        request.put("contentPartnerName", "Org1");
        request.put("email", "org1@gmail.com");

        when(registrationRepository.findByContentPartnerOrganizationName("Org1"))
                .thenReturn(Optional.empty());
        when(registrationRepository.findByContentPartnerEmail("org1@gmail.com"))
                .thenReturn(Optional.empty());

        ContentPartnerRegistrationEntity saved = new ContentPartnerRegistrationEntity();
        saved.setId(UUID.randomUUID().toString());
        saved.setCreatedOn(new Timestamp(System.currentTimeMillis()));
        saved.setUpdatedOn(saved.getCreatedOn());
        saved.setData(request);

        when(registrationRepository.save(any(ContentPartnerRegistrationEntity.class)))
                .thenReturn(saved);
        when(objectMapper.convertValue(any(), eq(Map.class))).thenReturn(new HashMap<>());
        when(cbServerProperties.getElasticContentPartnerJsonPath()).thenReturn("elastic-path");

        ApiResponse response = service.createOrUpdate(request);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        verify(registrationRepository).save(any());
        verify(esUtilService).addDocument(anyString(), anyString(), anyString(), anyMap(), anyString());
        verify(cacheService).putCache(anyString(), any());
    }

    @Test
    void testCreate_OrgNameExists() {
        ObjectNode req = realMapper.createObjectNode();
        req.put("contentPartnerName", "ExistingOrg");
        req.put("email", "new@gmail.com");

        when(registrationRepository.findByContentPartnerOrganizationName("ExistingOrg"))
                .thenReturn(Optional.of(new ContentPartnerRegistrationEntity()));

        ApiResponse response = service.createOrUpdate(req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals("Organization Name already registered", response.getParams().getErrMsg());
    }

    @Test
    void testCreate_EmailExists() {
        ObjectNode req = realMapper.createObjectNode();
        req.put("contentPartnerName", "Org2");
        req.put("email", "existing@gmail.com");

        when(registrationRepository.findByContentPartnerOrganizationName("Org2"))
                .thenReturn(Optional.empty());

        when(registrationRepository.findByContentPartnerEmail("existing@gmail.com"))
                .thenReturn(Optional.of(new ContentPartnerRegistrationEntity()));

        ApiResponse response = service.createOrUpdate(req);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals("Email already registered", response.getParams().getErrMsg());
    }


    @Test
    void testCreate_ValidationException() {
        ObjectNode req = realMapper.createObjectNode();
        req.put("contentPartnerName", "OrgX");

        doThrow(new RuntimeException("validation failed"))
                .when(payloadValidation)
                .validatePayload(anyString(), any());

        ApiResponse response = service.createOrUpdate(req);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertTrue(response.getParams().getErrMsg().contains("validation failed"));
    }


    // UPDATE TEST CASES

    @Test
    void testUpdate_Success() {
        ObjectNode req = realMapper.createObjectNode();
        req.put("id", "123");
        req.put("status", Constants.APPROVED);

        ContentPartnerRegistrationEntity existing = new ContentPartnerRegistrationEntity();
        existing.setId("123");

        ObjectNode data = realMapper.createObjectNode();
        data.put("contentPartnerName", "Org1");
        data.put("status", Constants.PENDING);
        existing.setData(data);

        when(registrationRepository.findById("123")).thenReturn(Optional.of(existing));
        when(registrationRepository.save(any())).thenReturn(existing);
        when(objectMapper.convertValue(any(), eq(Map.class))).thenReturn(new HashMap<>());
        when(cbServerProperties.getElasticContentPartnerJsonPath()).thenReturn("path");

        ApiResponse resp = service.createOrUpdate(req);

        assertEquals(HttpStatus.OK, resp.getResponseCode());
        verify(esUtilService).updateDocument(anyString(), anyString(), anyString(), anyMap(), anyString());
        verify(cacheService).putCache(eq("123"), any());
    }

    @Test
    void testUpdate_InvalidStatus() {
        ObjectNode req = realMapper.createObjectNode();
        req.put("id", "123");
        req.put("status", "INVALID_VALUE");

        ApiResponse resp = service.createOrUpdate(req);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getResponseCode());
        assertEquals("Invalid status. Allowed values: APPROVED, REJECTED", resp.getParams().getErrMsg());
    }

    @Test
    void testUpdate_MissingFields() {
        ObjectNode req = realMapper.createObjectNode();
        req.put("id", "123");

        ApiResponse resp = service.createOrUpdate(req);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getResponseCode());
        assertEquals("id and status are required", resp.getParams().getErrMsg());
    }

    @Test
    void testUpdate_NotFound() {
        ObjectNode req = realMapper.createObjectNode();
        req.put("id", "missing-id");
        req.put("status", Constants.APPROVED);

        when(registrationRepository.findById("missing-id")).thenReturn(Optional.empty());

        ApiResponse resp = service.createOrUpdate(req);

        assertEquals(HttpStatus.NOT_FOUND, resp.getResponseCode());
        assertEquals("Content Partner Registration not found", resp.getParams().getErrMsg());
    }

// READ TEST CASES

    @Test
    void testRead_Success_FromCache() throws Exception {
        String id = "test-id-123";
        Map<String, Object> cachedData = new HashMap<>();
        cachedData.put("id", id);
        cachedData.put("contentPartnerName", "Org1");
        cachedData.put("email", "org1@gmail.com");
        cachedData.put("status", Constants.PENDING);

        String cachedJson = realMapper.writeValueAsString(cachedData);

        when(cacheService.getCache(id)).thenReturn(cachedJson);
        when(objectMapper.readValue(eq(cachedJson), any(TypeReference.class)))
                .thenReturn(cachedData);

        ApiResponse response = service.read(id);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertNotNull(response.getResult());
        assertEquals(cachedData, response.getResult());
        verify(cacheService).getCache(id);
        verify(registrationRepository, never()).findById(anyString());
    }

    @Test
    void testRead_Success_FromDatabase() {
        String id = "test-id-456";

        ContentPartnerRegistrationEntity entity = new ContentPartnerRegistrationEntity();
        entity.setId(id);

        ObjectNode data = realMapper.createObjectNode();
        data.put("id", id);
        data.put("contentPartnerName", "Org2");
        data.put("email", "org2@gmail.com");
        data.put("status", Constants.APPROVED);
        entity.setData(data);
        entity.setCreatedOn(new Timestamp(System.currentTimeMillis()));
        entity.setUpdatedOn(new Timestamp(System.currentTimeMillis()));

        when(cacheService.getCache(id)).thenReturn(null);
        when(registrationRepository.findById(id)).thenReturn(Optional.of(entity));

        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("id", id);
        expectedResult.put("contentPartnerName", "Org2");

        when(objectMapper.convertValue(entity, Map.class)).thenReturn(expectedResult);

        ApiResponse response = service.read(id);

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertNotNull(response.getResult());
        verify(cacheService).getCache(id);
        verify(registrationRepository).findById(id);
        verify(cacheService).putCache(eq(id), eq(entity));
    }

    @Test
    void testRead_NotFound() {
        String id = "non-existent-id";

        when(cacheService.getCache(id)).thenReturn(null);
        when(registrationRepository.findById(id)).thenReturn(Optional.empty());

        ApiResponse response = service.read(id);

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.INVALID_ID, response.getParams().getErrMsg());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        verify(cacheService).getCache(id);
        verify(registrationRepository).findById(id);
    }

    @Test
    void testRead_EmptyId() {
        ApiResponse response = service.read("");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals(Constants.ID_NOT_FOUND, response.getParams().getErrMsg());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        verify(cacheService, never()).getCache(anyString());
        verify(registrationRepository, never()).findById(anyString());
    }

    @Test
    void testRead_NullId() {
        ApiResponse response = service.read(null);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals(Constants.ID_NOT_FOUND, response.getParams().getErrMsg());
        assertEquals(Constants.FAILED, response.getParams().getStatus());
        verify(cacheService, never()).getCache(anyString());
        verify(registrationRepository, never()).findById(anyString());
    }

    @Test
    void testRead_CacheException() throws Exception {
        String id = "test-id-789";
        String cachedJson = "invalid-json";

        when(cacheService.getCache(id)).thenReturn(cachedJson);
        when(objectMapper.readValue(eq(cachedJson), any(TypeReference.class)))
                .thenThrow(new RuntimeException("JSON parsing error"));

        ApiResponse response = service.read(id);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertTrue(response.getParams().getErrMsg().contains("JSON parsing error"));
        assertEquals(Constants.FAILED, response.getParams().getStatus());
    }

    @Test
    void testRead_DatabaseException() {
        String id = "test-id-999";

        when(cacheService.getCache(id)).thenReturn(null);
        when(registrationRepository.findById(id))
                .thenThrow(new RuntimeException("Database connection failed"));

        ApiResponse response = service.read(id);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertTrue(response.getParams().getErrMsg().contains("Database connection failed"));
        assertEquals(Constants.FAILED, response.getParams().getStatus());
    }

}
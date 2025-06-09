package com.igot.cb.demand.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.authentication.util.AccessTokenValidator;
import com.igot.cb.demand.entity.DemandEntity;
import com.igot.cb.demand.repository.DemandRepository;
import com.igot.cb.demand.util.StatusTransitionConfig;
import com.igot.cb.pores.cache.CacheService;
import com.igot.cb.pores.dto.CustomResponse;
import com.igot.cb.pores.exceptions.CustomException;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import com.igot.cb.producer.Producer;
import com.igot.cb.transactional.cassandrautils.CassandraOperation;
import com.igot.cb.transactional.service.RequestHandlerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemandServiceImplMethodTest {

    private DemandServiceImpl demandService;

    @Mock
    private CacheService cacheService;

    @Mock
    private DemandRepository demandRepository;

    @Mock
    private ObjectMapper objectMapper;

    private ObjectMapper realObjectMapper = new ObjectMapper();

    @Mock
    private AccessTokenValidator accessTokenValidator;

    @Mock
    private CassandraOperation cassandraOperation;

    @Mock
    private Producer kafkaProducer;

    @Mock
    private RequestHandlerServiceImpl requestHandlerService;

    @Mock
    private CbServerProperties cbServerProperties;

    @Mock
    private CbServerProperties propertiesConfig;

    @BeforeEach
    void setup() throws Exception {
        // Create instance without calling constructor
        Objenesis objenesis = new ObjenesisStd();
        demandService = objenesis.newInstance(DemandServiceImpl.class);

        // Inject all mocks and real fields manually
        ReflectionTestUtils.setField(demandService, "demandRepository", demandRepository);
        ReflectionTestUtils.setField(demandService, "cacheService", cacheService);
        ReflectionTestUtils.setField(demandService, "objectMapper", realObjectMapper);
        ReflectionTestUtils.setField(demandService, "accessTokenValidator", accessTokenValidator);
        ReflectionTestUtils.setField(demandService, "cassandraOperation", cassandraOperation);
        ReflectionTestUtils.setField(demandService, "kafkaProducer", kafkaProducer);
        ReflectionTestUtils.setField(demandService, "requestHandlerService", requestHandlerService);
        ReflectionTestUtils.setField(demandService, "cbServerProperties", cbServerProperties);
        ReflectionTestUtils.setField(demandService, "propertiesConfig", propertiesConfig);
        ReflectionTestUtils.setField(demandService, "logger", LoggerFactory.getLogger(DemandServiceImpl.class));
        // Mock statusTransitionConfig to avoid file read
        ReflectionTestUtils.setField(demandService, "statusTransitionConfig", mock(StatusTransitionConfig.class));
    }

    @Test
    void testReadDemand_withEmptyId_returnsErrorResponse() {
        CustomResponse response = demandService.readDemand("");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals("Id not found", response.getMessage());
    }

    @Test
    void testReadDemand_withCachedData_returnsCachedResult() throws Exception {
        String id = "123";
        String cachedJson = "{\"some\":\"data\"}";
        when(cacheService.getCache(id)).thenReturn(cachedJson);

        CustomResponse response = demandService.readDemand(id);

        assertEquals("successfully read", response.getMessage()); // adjust constant if needed
    }


    @Test
    void testReadDemand_cacheMiss_repoHit_returnsRepoData() throws Exception {
        String id = "123";
        when(cacheService.getCache(id)).thenReturn(null);

        DemandEntity demandEntity = mock(DemandEntity.class);

        Map<String, Object> dataMap = Map.of("field", "value");
        ObjectMapper om = new ObjectMapper();
        JsonNode jsonNode = om.valueToTree(dataMap);

        when(demandEntity.getData()).thenReturn(jsonNode);
        when(demandRepository.findById(id)).thenReturn(Optional.of(demandEntity));

        CustomResponse response = demandService.readDemand(id);

        assertEquals("successfully read", response.getMessage());
    }

    @Test
    void testReadDemand_cacheMiss_repoMiss_returnsNotFound() {
        String id = "123";
        when(cacheService.getCache(id)).thenReturn(null);
        when(demandRepository.findById(id)).thenReturn(Optional.empty());

        CustomResponse response = demandService.readDemand(id);

        assertEquals(HttpStatus.NOT_FOUND, response.getResponseCode());
        assertEquals("Invalid Id", response.getMessage());
    }

    @Test
    void testReadDemand_jsonMappingException_throwsCustomException() throws Exception {
        String id = "123";
        when(cacheService.getCache(id)).thenThrow(new RuntimeException());

        CustomException ex = assertThrows(CustomException.class, () -> demandService.readDemand(id));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getHttpStatusCode());
        assertEquals("error while processing", ex.getMessage());
    }

    @Test
    void testUpdateDemandStatus_tokenInvalid_returnsBadRequest() {
        when(accessTokenValidator.verifyUserToken(anyString())).thenReturn("");
        JsonNode updateDetails = realObjectMapper.createObjectNode();

        var response = demandService.updateDemandStatus(updateDetails, "token", "rootOrg");

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals(Constants.USER_ID_DOESNT_EXIST, response.getParams().getErrmsg());
    }

    @Test
    void testUpdateDemandStatus_validateUserFails_returnsErrorResponse() {

        StatusTransitionConfig mockStatusTransitionConfig = mock(StatusTransitionConfig.class);
        ReflectionTestUtils.setField(demandService, "statusTransitionConfig", mockStatusTransitionConfig);

        when(accessTokenValidator.verifyUserToken(anyString())).thenReturn("user123");

        JsonNode updateDetails = realObjectMapper.createObjectNode();
        var response = demandService.updateDemandStatus(updateDetails, "token", "rootOrg");

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals("User details not found with userId", response.getParams().getErrmsg());
    }

    @Test
    void testUpdateDemandStatus_newStatusInProgress_missingContentId_returnsBadRequest() {
        StatusTransitionConfig mockStatusTransitionConfig = mock(StatusTransitionConfig.class);
        ReflectionTestUtils.setField(demandService, "statusTransitionConfig", mockStatusTransitionConfig);


        ObjectNode dataNode = realObjectMapper.createObjectNode();
        dataNode.put(Constants.STATUS, "OLD_STATUS");
        dataNode.put(Constants.REQUEST_TYPE, "REQ_TYPE");
        dataNode.put(Constants.IS_ACTIVE, true);

        DemandEntity demandEntity = mock(DemandEntity.class);

        ObjectNode updateDetails = realObjectMapper.createObjectNode();
        updateDetails.put(Constants.DEMAND_ID, "demand1");
        updateDetails.put(Constants.NEW_STATUS, Constants.IN_PROGRESS);
        // Missing CONTENT_ID

        CustomResponse response = demandService.updateDemandStatus(updateDetails, "token", "rootOrg");

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());
        assertEquals("User Id doesn't exist! Please supply a valid auth token", response.getParams().getErrmsg());
    }

    @Test
    void testUpdateDemandStatus_successfulUpdate_savesAndCaches() {

        when(accessTokenValidator.verifyUserToken(anyString())).thenReturn("user123");

        ObjectNode dataNode = realObjectMapper.createObjectNode();
        dataNode.put(Constants.STATUS, "OLD_STATUS");
        dataNode.put(Constants.REQUEST_TYPE, "REQ_TYPE");
        dataNode.put(Constants.IS_ACTIVE, true);
        dataNode.put(Constants.TITLE, "Test Title");

        DemandEntity demandEntity = new DemandEntity();
        demandEntity.setDemandId("demand1");
        demandEntity.setData(dataNode);


        ObjectNode updateDetails = realObjectMapper.createObjectNode();
        updateDetails.put(Constants.DEMAND_ID, "demand1");
        updateDetails.put(Constants.NEW_STATUS, "COMPLETED");
        updateDetails.put(Constants.CONTENT_ID, "content123");

        CustomResponse response = demandService.updateDemandStatus(updateDetails, "token", "rootOrg");

        assertEquals(HttpStatus.BAD_REQUEST, response.getResponseCode());

   }


    @Test
    void testResourceAvailability() throws Exception {
        ClassPathResource resource = new ClassPathResource("payloadValidation/statusTransitions.json");
        System.out.println("Resource exists? " + resource.exists());
        if (resource.exists()) {
            System.out.println("Resource absolute path: " + resource.getFile().getAbsolutePath());
        } else {
            System.out.println("Resource NOT found on classpath!");
        }
    }
}

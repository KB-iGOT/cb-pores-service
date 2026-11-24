package com.igot.cb.cios.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.cios.entity.CiosContentEntity;
import com.igot.cb.cios.repository.CiosRepository;
import com.igot.cb.pores.cache.CacheService;
import com.igot.cb.pores.elasticsearch.service.EsUtilService;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseDataWriterTest {

    private static final Timestamp NOW = Timestamp.valueOf("2026-11-22 10:15:30");
    private static final Timestamp EARLIER = Timestamp.valueOf("2026-01-01 08:00:00");

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private CiosRepository ciosRepository;
    @Mock
    private EsUtilService esUtilService;
    @Mock
    private CacheService cacheService;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private CbServerProperties cbServerProperties;

    private CourseDataWriter store;
    private ObjectNode courseData;

    @BeforeEach
    void setUp() {
        store = new CourseDataWriter(ciosRepository, mapper, esUtilService, cacheService, restTemplate,
                cbServerProperties);
        courseData = mapper.createObjectNode();
        ObjectNode content = courseData.putObject("content");
        content.put("externalId", "ext-1");
        content.put("contentId", "ext_new");
        content.put("status", "live");
        content.putObject("contentPartner").put("id", "partner-1");
    }

    private JsonNode content() {
        return courseData.path("content");
    }

    private CiosContentEntity savedEntity() {
        ArgumentCaptor<CiosContentEntity> captor = ArgumentCaptor.forClass(CiosContentEntity.class);
        verify(ciosRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void sendToCiosStaging_postsTheDataAsJsonToTheCiosUpdateApi() {
        when(cbServerProperties.getCiosContentServiceHost()).thenReturn("http://cios");
        when(cbServerProperties.getCiosContentServiceUpdateApiUrl()).thenReturn("/update");

        store.sendToCiosStaging(courseData);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<JsonNode>> entity = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(eq("http://cios/update"), entity.capture(), eq(JsonNode.class));
        assertEquals(courseData, entity.getValue().getBody());
        assertEquals(MediaType.APPLICATION_JSON, entity.getValue().getHeaders().getContentType());
    }

    @Test
    void saveInLiveTableCacheAndIndex_newCourse_isSavedCachedUnderBothKeysAndIndexed() {
        when(ciosRepository.findByExternalIdAndPartnerId("ext-1", "partner-1")).thenReturn(Optional.empty());
        when(cbServerProperties.getElasticCiosJsonPath()).thenReturn("/es.json");

        store.saveInLiveTableCacheAndIndex(courseData, NOW);

        CiosContentEntity entity = savedEntity();
        assertEquals("ext_new", entity.getContentId());
        assertEquals("ext-1", entity.getExternalId());
        assertEquals("partner-1", entity.getPartnerId());
        assertTrue(entity.getIsActive());
        assertEquals(NOW, entity.getCreatedOn());
        assertEquals(NOW, entity.getLastUpdatedOn());
        assertEquals(courseData, entity.getCiosData());
        assertEquals(Constants.LIVE, content().path("status").asText());
        assertEquals(NOW.toString(), content().path("createdOn").asText());
        assertEquals(NOW.toString(), content().path("lastUpdatedOn").asText());
        verify(cacheService).putCache("ext_new", courseData);
        verify(cacheService).putCache("ext-1_partner-1", courseData);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> document = ArgumentCaptor.forClass(Map.class);
        verify(esUtilService).addDocument(eq(Constants.CIOS_INDEX_NAME), eq(Constants.INDEX_TYPE), eq("ext_new"),
                document.capture(), eq("/es.json"));
        assertEquals("ext-1", document.getValue().get("externalId"));
    }

    @Test
    void saveInLiveTableCacheAndIndex_courseOnboardedBefore_keepsItsContentIdAndCreationTime() {
        CiosContentEntity existing = new CiosContentEntity();
        existing.setContentId("ext_old");
        existing.setCreatedOn(EARLIER);
        when(ciosRepository.findByExternalIdAndPartnerId("ext-1", "partner-1")).thenReturn(Optional.of(existing));

        store.saveInLiveTableCacheAndIndex(courseData, NOW);

        CiosContentEntity entity = savedEntity();
        assertEquals("ext_old", entity.getContentId());
        assertEquals(EARLIER, entity.getCreatedOn());
        assertEquals(NOW, entity.getLastUpdatedOn());
        assertEquals("ext_old", content().path("contentId").asText());
        assertEquals(EARLIER.toString(), content().path("createdOn").asText());
        verify(cacheService).putCache("ext_old", courseData);
        verify(esUtilService).addDocument(any(), any(), eq("ext_old"), any(), any());
    }
}

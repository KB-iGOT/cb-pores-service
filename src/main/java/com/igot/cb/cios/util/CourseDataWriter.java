package com.igot.cb.cios.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.cios.entity.CiosContentEntity;
import com.igot.cb.cios.repository.CiosRepository;
import com.igot.cb.pores.cache.CacheService;
import com.igot.cb.pores.elasticsearch.service.EsUtilService;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;

/**
 * Writes a course to where it lives: the cios staging service for every course, plus the live table, Redis cache and live index for a live one.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseDataWriter {

    private static final TypeReference<Map<String, Object>> DOCUMENT_TYPE = new TypeReference<>() {
    };

    private final CiosRepository ciosRepository;
    private final ObjectMapper objectMapper;
    private final EsUtilService esUtilService;
    private final CacheService cacheService;
    private final RestTemplate restTemplate;
    private final CbServerProperties cbServerProperties;

    /**
     * Sends the course to cios, which saves it in its staging table and onboarding index; a draft stops here.
     */
    public void sendToCiosStaging(ObjectNode courseData) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String url = cbServerProperties.getCiosContentServiceHost() + cbServerProperties.getCiosContentServiceUpdateApiUrl();
        log.debug("CourseDataWriter::sendToCiosStaging: posting externalId {} to {}",
                courseData.path(Constants.CONTENT).path(Constants.EXTERNAL_ID).asText(), url);
        try {
            restTemplate.postForObject(url, new HttpEntity<>(courseData, headers), JsonNode.class);
        } catch (RuntimeException e) {
            log.error("CourseDataWriter::sendToCiosStaging: cios staging call to {} failed", url, e);
            throw e;
        }
    }

    /**
     * Saves the live course in the live table, under both Redis keys and in the live index.
     */
    public void saveInLiveTableCacheAndIndex(ObjectNode courseData, Timestamp now) {
        ObjectNode content = (ObjectNode) courseData.path(Constants.CONTENT);
        CiosContentEntity entity = buildLiveTableRow(courseData, content, now);
        ciosRepository.save(entity);
        log.info("CourseDataWriter::saveInLiveTableCacheAndIndex: saved content {}", entity.getContentId());
        cacheService.putCache(entity.getContentId(), courseData);
        cacheService.putCache(entity.getExternalId() + "_" + entity.getPartnerId(), courseData);
        esUtilService.addDocument(Constants.CIOS_INDEX_NAME, Constants.INDEX_TYPE, entity.getContentId(),
                objectMapper.convertValue(content, DOCUMENT_TYPE), cbServerProperties.getElasticCiosJsonPath());
    }

    /**
     * Builds the table row. A course onboarded before keeps its content id and creation time, and the content gets the
     * same ids, dates and status as the row so the table, cache and index agree.
     */
    private CiosContentEntity buildLiveTableRow(ObjectNode courseData, ObjectNode content, Timestamp now) {
        String externalId = content.path(Constants.EXTERNAL_ID).asText();
        String partnerId = content.path(Constants.CONTENT_PARTNER).path(Constants.ID).asText();
        Optional<CiosContentEntity> existing = ciosRepository.findByExternalIdAndPartnerId(externalId, partnerId);
        log.debug("CourseDataWriter::buildLiveTableRow: externalId {} partnerId {} {}", externalId, partnerId,
                existing.isPresent() ? "already live, keeping its contentId" : "is new in the live table");
        CiosContentEntity entity = new CiosContentEntity();
        entity.setContentId(existing.map(CiosContentEntity::getContentId)
                .orElseGet(() -> content.path(Constants.CONTENT_ID).asText()));
        entity.setExternalId(externalId);
        entity.setPartnerId(partnerId);
        entity.setIsActive(Constants.ACTIVE_STATUS);
        entity.setCreatedOn(existing.map(CiosContentEntity::getCreatedOn).orElse(now));
        entity.setLastUpdatedOn(now);
        entity.setCiosData(courseData);
        content.put(Constants.CONTENT_ID, entity.getContentId());
        content.put(Constants.CREATED_ON, String.valueOf(entity.getCreatedOn()));
        content.put(Constants.LAST_UPDATED_ON, String.valueOf(now));
        content.put(Constants.STATUS, Constants.LIVE);
        return entity;
    }
}

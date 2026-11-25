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
import org.springframework.dao.IncorrectResultSizeDataAccessException;
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
        String externalId = courseData.path(Constants.CONTENT).path(Constants.EXTERNAL_ID).asText();
        log.info("CourseDataWriter::sendToCiosStaging: calling cios staging {} for externalId {}", url, externalId);
        try {
            restTemplate.postForObject(url, new HttpEntity<>(courseData, headers), JsonNode.class);
            log.info("CourseDataWriter::sendToCiosStaging: cios staging accepted externalId {}", externalId);
        } catch (RuntimeException e) {
            log.error("CourseDataWriter::sendToCiosStaging: cios staging call to {} failed for externalId {}",
                    url, externalId, e);
            throw e;
        }
    }

    /**
     * Saves the live course in the live table, under both Redis keys and in the live index.
     */
    public void saveInLiveTableCacheAndIndex(ObjectNode courseData, Timestamp now) {
        ObjectNode content = (ObjectNode) courseData.path(Constants.CONTENT);
        CiosContentEntity entity = buildLiveTableRow(courseData, content, now);
        log.info("CourseDataWriter::saveInLiveTableCacheAndIndex: saving the live row, contentId {}, externalId {}, partnerId {}",
                entity.getContentId(), entity.getExternalId(), entity.getPartnerId());
        ciosRepository.save(entity);
        log.info("CourseDataWriter::saveInLiveTableCacheAndIndex: live row saved, contentId {}", entity.getContentId());
        String externalIdAndPartnerIdKey = entity.getExternalId() + "_" + entity.getPartnerId();
        cacheService.putCache(entity.getContentId(), courseData);
        cacheService.putCache(externalIdAndPartnerIdKey, courseData);
        log.info("CourseDataWriter::saveInLiveTableCacheAndIndex: cached under the keys {} and {}",
                entity.getContentId(), externalIdAndPartnerIdKey);
        esUtilService.addDocument(Constants.CIOS_INDEX_NAME, Constants.INDEX_TYPE, entity.getContentId(),
                objectMapper.convertValue(content, DOCUMENT_TYPE), cbServerProperties.getElasticCiosJsonPath());
        log.info("CourseDataWriter::saveInLiveTableCacheAndIndex: live index {} updated for contentId {}",
                Constants.CIOS_INDEX_NAME, entity.getContentId());
    }

    /**
     * Builds the table row. A course onboarded before keeps its content id and creation time, and the content gets the
     * same ids, dates and status as the row so the table, cache and index agree.
     */
    private CiosContentEntity buildLiveTableRow(ObjectNode courseData, ObjectNode content, Timestamp now) {
        String externalId = content.path(Constants.EXTERNAL_ID).asText();
        String partnerId = content.path(Constants.CONTENT_PARTNER).path(Constants.ID).asText();
        Optional<CiosContentEntity> existing = findLiveRow(externalId, partnerId);
        log.info("CourseDataWriter::buildLiveTableRow: externalId {} partnerId {}: {}", externalId, partnerId,
                existing.map(row -> "already live as contentId " + row.getContentId() + ", keeping that contentId")
                        .orElse("not in the live table yet, a new row is created"));
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

    /**
     * Looks up the live row of the course. More than one row for the same externalId and partnerId is a data problem,
     * so it is logged with both ids before the exception goes on.
     */
    private Optional<CiosContentEntity> findLiveRow(String externalId, String partnerId) {
        log.info("CourseDataWriter::findLiveRow: reading the live row of externalId {} and partnerId {}", externalId, partnerId);
        try {
            return ciosRepository.findByExternalIdAndPartnerId(externalId, partnerId);
        } catch (IncorrectResultSizeDataAccessException e) {
            log.error("CourseDataWriter::findLiveRow: the live table has more than one row for externalId {} and partnerId {}",
                    externalId, partnerId, e);
            throw e;
        }
    }
}

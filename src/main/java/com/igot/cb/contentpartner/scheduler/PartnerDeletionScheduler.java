package com.igot.cb.contentpartner.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.cios.dto.ObjectDto;
import com.igot.cb.cios.service.CiosContentService;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import com.igot.cb.pores.elasticsearch.dto.SearchResult;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.time.Duration;
import java.util.*;

@Component
public class PartnerDeletionScheduler {

    private static final Logger log = LoggerFactory.getLogger(PartnerDeletionScheduler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final KafkaConsumer<String, String> partnerDeleteKafkaConsumer;
    private final CiosContentService ciosContentService;
    private final CiosContentService onboardContentService;
    private final CbServerProperties cbServerProperties;

    public PartnerDeletionScheduler(
            KafkaConsumer<String, String> partnerDeleteKafkaConsumer,
            CiosContentService ciosContentService,
            CiosContentService onboardContentService,
            CbServerProperties cbServerProperties
    ) {
        this.partnerDeleteKafkaConsumer = partnerDeleteKafkaConsumer;
        this.ciosContentService = ciosContentService;
        this.onboardContentService = onboardContentService;
        this.cbServerProperties = cbServerProperties;
    }

    @PostConstruct
    public void init() {
        String topic = cbServerProperties.getContentPartnerDeleteTopic();
        log.info("PartnerDeletionScheduler subscribing to topic: {}", topic);
        partnerDeleteKafkaConsumer.subscribe(Collections.singletonList(topic));
    }

    @Scheduled(cron = "${partner.deletion.scheduler.cron}")
    public void processDeletedPartners() {
        try {
            boolean moreRecords = true;
            while (moreRecords) {
                ConsumerRecords<String, String> records = partnerDeleteKafkaConsumer.poll(Duration.ofSeconds(2));

                if (records.isEmpty()) {
                    log.info(Constants.NO_PARTNER_IDS_TO_PROCESS);
                    moreRecords = false;
                    continue;
                }
                for (ConsumerRecord<String, String> record : records) {
                    String partnerId = null;
                    try {
                        Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);
                        partnerId = (String) event.get(Constants.PARTNER_ID);

                        if (partnerId == null || partnerId.isBlank()) {
                            log.warn(Constants.SKIPPING_EMPTY_PARTNER_ID, record.value());
                            partnerDeleteKafkaConsumer.commitSync();
                            continue;
                        }
                        List<String> contentIds = fetchAllContentIdsByPartner(partnerId);
                        if (contentIds.isEmpty()) {
                            partnerDeleteKafkaConsumer.commitSync();
                            continue;
                        }
                        deactivateCoursesByContentIds(contentIds);
                        partnerDeleteKafkaConsumer.commitSync();
                        log.info(Constants.PROCESSED_PARTNER_ID, partnerId);
                    } catch (Exception ex) {
                        log.error(Constants.FAILED_PROCESSING_PARTNER_ID_WILL_RETRY, partnerId, ex);
                    }
                }
            }
        } catch (Exception e) {
            log.error(Constants.SCHEDULER_POLL_FAILED, e);
        }
    }

    private List<String> fetchAllContentIdsByPartner(String partnerId) {
        int page = 0;
        int size = 500;
        List<String> allContentIds = new ArrayList<>();
        while (true) {
            SearchCriteria criteria = new SearchCriteria();
            HashMap<String, Object> filterMap = new HashMap<>();
            filterMap.put(Constants.FILTER_CONTENT_PARTNER_ID, partnerId);
            criteria.setFilterCriteriaMap(filterMap);
            criteria.setRequestedFields(List.of(Constants.CONTENT_ID));
            criteria.setPageNumber(page);
            criteria.setPageSize(size);
            SearchResult result = ciosContentService.searchCotent(criteria);
            if (result == null || result.getData() == null || result.getData().isEmpty()) {
                break;
            }
            for (JsonNode node : result.getData()) {
                JsonNode cid = node.get(Constants.CONTENT_ID);
                if (cid != null && !cid.isNull()) {
                    String contentId = cid.asText();
                    if (contentId != null && !contentId.isBlank()) {
                        allContentIds.add(contentId);
                    }
                }
            }
            if (result.getData().size() < size) break;
            page++;
        }

        return allContentIds;
    }

    private void deactivateCoursesByContentIds(List<String> contentIds) {
        if (contentIds == null || contentIds.isEmpty()) return;
        List<ObjectDto> payload = new ArrayList<>();
        for (String contentId : contentIds) {
            try {
                Object response = ciosContentService.fetchDataByContentId(contentId);
                if (response == null) {
                    log.warn(Constants.NO_RESPONSE_FOR_CONTENT_ID, contentId);
                    continue;
                }
                JsonNode root = objectMapper.valueToTree(response);
                JsonNode contentNode = root.get(Constants.CONTENT);
                if (contentNode == null || contentNode.isNull() || !contentNode.isObject()) {
                    log.warn(Constants.NO_VALID_CONTENT_NODE_FOR_CONTENT_ID, contentId);
                    continue;
                }
                Map<String, Object> content = objectMapper.convertValue(contentNode, Map.class);
                ObjectDto dto = buildDeactivateDtoFromContent(content);
                payload.add(dto);
            } catch (Exception e) {
                log.error(Constants.FAILED_TO_FETCH_OR_BUILD_DTO_FOR_CONTENT_ID, contentId, e);
            }
        }
        if (!payload.isEmpty()) {
            onboardContentService.onboardContent(payload);
        }
    }

    private ObjectDto buildDeactivateDtoFromContent(Map<String, Object> content) {
        ObjectDto dto = new ObjectDto();
        Object statusObj = content.get(Constants.STATUS);
        String status = statusObj == null ? "live" : statusObj.toString().trim().toLowerCase();
        dto.setStatus(status);
        Object cpObj = content.get(Constants.CONTENT_PARTNER);
        Map<String, Object> contentPartner = (cpObj instanceof Map) ? new HashMap<>((Map<String, Object>) cpObj) : new HashMap<>();
        contentPartner.put(Constants.IS_ACTIVE, Constants.ACTIVE_STATUS_FALSE);
        dto.setContentPartner(objectMapper.valueToTree(contentPartner));
        Map<String, Object> contentMap = new HashMap<>();
        contentMap.put(Constants.NAME, content.get(Constants.NAME));
        contentMap.put(Constants.COURSE_APP_ICON, content.get(Constants.COURSE_APP_ICON));
        Object duration = content.get(Constants.DURATION);
        contentMap.put(Constants.DURATION, duration == null ? "" : String.valueOf(duration));
        contentMap.put(Constants.IS_ACTIVE, content.get(Constants.IS_ACTIVE));
        contentMap.put(Constants.CONTENT_ID, content.get(Constants.CONTENT_ID));
        contentMap.put(Constants.CREATED_ON, content.get(Constants.CREATED_ON));
        contentMap.put(Constants.EXTERNAL_ID, content.get(Constants.EXTERNAL_ID));
        contentMap.put(Constants.OBJECTIVE, content.getOrDefault(Constants.OBJECTIVE, content.get(Constants.NAME)));
        contentMap.put(Constants.DESCRIPTION, content.get(Constants.DESCRIPTION));
        contentMap.put(Constants.REDIRECT_URL, content.get(Constants.REDIRECT_URL));
        contentMap.put(Constants.LAST_UPDATED_ON, content.get(Constants.LAST_UPDATED_ON));
        dto.setContentData(objectMapper.valueToTree(Map.of(Constants.CONTENT, contentMap)));
        dto.setCompetencies_v6(objectMapper.valueToTree(content.getOrDefault(Constants.COMPETENCIES_V6, List.of())));
        dto.setAccessSettingsEnabled((Boolean) content.getOrDefault(Constants.ACCESS_SETTINGS_ENABLED, false));
        dto.setTags(List.of());

        return dto;
    }
}
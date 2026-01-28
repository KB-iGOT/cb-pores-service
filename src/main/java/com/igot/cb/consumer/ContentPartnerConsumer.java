package com.igot.cb.consumer;

import com.igot.cb.cios.service.CiosContentService;
import com.igot.cb.pores.elasticsearch.dto.SearchCriteria;
import com.igot.cb.pores.elasticsearch.dto.SearchResult;
import com.igot.cb.pores.util.Constants;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ContentPartnerConsumer {

    private static final Logger log = LoggerFactory.getLogger(ContentPartnerConsumer.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CiosContentService ciosContentService;

    @Value("${content.partner.search.page-size}")
    private int pageSize;

    // ---------- DEACTIVATE ----------
    @KafkaListener(topics = "${kafka.topic.content.partner.delete}", groupId = "${kafka.topic.content.partner.delete.group}")
    public void consumePartnerDeactivate(ConsumerRecord<String, String> record) {
        log.info("Partner DEACTIVATE event received: {}", record.value());
        handlePartnerEvent(record, false);
    }

    // ---------- ACTIVATE ----------
    @KafkaListener(topics = "${kafka.topic.content.partner.activate}", groupId = "${kafka.topic.content.partner.activate.group}")
    public void consumePartnerActivate(ConsumerRecord<String, String> record) {
        log.info("Partner ACTIVATE event received: {}", record.value());
        handlePartnerEvent(record, true);
    }

    private void handlePartnerEvent(ConsumerRecord<String, String> record, boolean isActive) {
        try {
            Map<String, Object> event = objectMapper.readValue(record.value(), Map.class);
            String partnerId = (String) event.get(Constants.PARTNER_ID);
            if (partnerId == null || partnerId.isBlank()) {
                log.warn("partnerId missing in event: {}", event);
                return;
            }
            updateAllPartnerContents(partnerId, isActive);
        } catch (Exception e) {
            log.error("Failed to consume partner event", e);
        }
    }

    private void updateAllPartnerContents(String partnerId, boolean isActive) {
        int page = 0;
        int size = pageSize;
        boolean continueProcessing = true;
        long processedCount = 0;
        long totalCount = 0;
        do {
            SearchCriteria criteria = new SearchCriteria();
            HashMap<String, Object> filterMap = new HashMap<>();
            filterMap.put(Constants.FILTER_CONTENT_PARTNER_ID, partnerId);
            filterMap.put(Constants.IS_ACTIVE, null);
            criteria.setFilterCriteriaMap(filterMap);
            criteria.setPageNumber(page);
            criteria.setPageSize(size);
            SearchResult result = ciosContentService.searchCotent(criteria);
            if (result == null || result.getData() == null || !result.getData().isArray()) {
                log.warn("Search failed or invalid response for partnerId={}", partnerId);
                continueProcessing = false;
                break;
            }
            if (result.getData().size() == 0) {
                continueProcessing = false;
                break;
            }
            if (page == 0) {
                totalCount = result.getTotalCount();
            }
            ciosContentService.updatePartnerIsActiveInEs(result.getData(), partnerId, isActive);
            processedCount += result.getData().size();
            if (processedCount >= totalCount) {
                continueProcessing = false;
                break;
            }
            page++;
        } while (continueProcessing);
    }
}
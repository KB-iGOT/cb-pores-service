package com.igot.cb.cios.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.contentpartner.service.ContentPartnerService;
import com.igot.cb.contentpartner.service.ContentPartnerServiceV2;
import com.igot.cb.pores.util.ApiResponse;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Updates the total, draft and live course counts saved on a partner, using the counts the search index reports. The
 * partner is read with the common partner service and written with the V2 partner service, because only V2 accepts
 * the LTI fields (protocol and ltiConfiguration) that an LTI partner carries.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PartnerCourseCountUpdater {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final CbServerProperties cbServerProperties;
    private final ContentPartnerService contentPartnerService;
    private final ContentPartnerServiceV2 contentPartnerServiceV2;

    /**
     * Updates the course counts on the partner. The course is already saved when this runs, so a failure here is
     * logged and does not fail the onboarding.
     */
    public void updateCourseCounts(String partnerCode) {
        try {
            JsonNode counts = searchCourseCountsByStatus(partnerCode);
            if (hasTotalAndStatusCounts(counts)) {
                writeCountsToPartner(partnerCode, counts);
            } else {
                log.warn("PartnerCourseCountUpdater::updateCourseCounts: no usable counts for partner {}", partnerCode);
            }
        } catch (RuntimeException e) {
            log.error("PartnerCourseCountUpdater::updateCourseCounts: unable to update the counts of partner {}", partnerCode, e);
        }
    }

    private JsonNode searchCourseCountsByStatus(String partnerCode) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.putObject(Constants.FILTER_CRITERIA_MAP).put(Constants.PARTNERCODE, partnerCode);
        payload.putArray(Constants.REQUESTED_FIELDS).add(Constants.EXTERNAL_ID);
        payload.putArray(Constants.FACETS).add(Constants.STATUS);
        payload.put(Constants.PAGE_NUMBER, 0);
        payload.put(Constants.PAGE_SIZE, 1);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForObject(
                cbServerProperties.getCiosContentServiceHost() + cbServerProperties.getCiosContentServiceSearchApiUrl(),
                new HttpEntity<>(payload, headers), JsonNode.class);
    }

    private static boolean hasTotalAndStatusCounts(JsonNode counts) {
        return counts != null && counts.hasNonNull(Constants.TOTAL_COUNT)
                && counts.path(Constants.FACETS).has(Constants.STATUS);
    }

    private void writeCountsToPartner(String partnerCode, JsonNode counts) {
        ApiResponse partnerResponse = contentPartnerService.getContentDetailsByPartnerCode(partnerCode);
        JsonNode partner = partnerResponse == null ? null : objectMapper.valueToTree(partnerResponse.getResult());
        if (partner == null || !partner.path(Constants.DATA).isObject()) {
            log.error("PartnerCourseCountUpdater::writeCountsToPartner: no partner data for {}", partnerCode);
            return;
        }
        JsonNode statusFacets = counts.path(Constants.FACETS).path(Constants.STATUS);
        ((ObjectNode) partner.path(Constants.DATA))
                .put(Constants.TOTAL_COURSES_COUNT, counts.path(Constants.TOTAL_COUNT).asLong())
                .put(Constants.DRAFT_COURSES_COUNT, countForStatus(statusFacets, Constants.DRAFT))
                .put(Constants.LIVE_COURSES_COUNT, countForStatus(statusFacets, Constants.LIVE_STATUS_LOWER_CASE));
        ApiResponse updateResponse = contentPartnerServiceV2.createOrUpdate(partner);
        if (updateResponse == null || updateResponse.getResponseCode() != HttpStatus.OK) {
            log.error("PartnerCourseCountUpdater::writeCountsToPartner: the partner {} was not updated: {}", partnerCode,
                    updateResponse == null ? "no response" : updateResponse.getParams().getErrMsg());
        }
    }

    private static long countForStatus(JsonNode statusFacets, String status) {
        for (JsonNode facet : statusFacets) {
            if (status.equalsIgnoreCase(facet.path(Constants.VALUE).asText())) {
                return facet.path(Constants.COUNT).asLong();
            }
        }
        return 0;
    }
}

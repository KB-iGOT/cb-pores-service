package com.igot.cb.cios.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.igot.cb.cios.dto.OnboardContentV2Request;
import com.igot.cb.cios.service.CiosContentServiceV2;
import com.igot.cb.cios.util.CoursePublisher;
import com.igot.cb.cios.util.LtiContentFieldsHelper;
import com.igot.cb.cios.util.PartnerCourseCountUpdater;
import com.igot.cb.contentpartner.service.ContentPartnerService;
import com.igot.cb.playlist.util.ProjectUtil;
import com.igot.cb.pores.exceptions.CustomException;
import com.igot.cb.pores.util.ApiResponse;
import com.igot.cb.pores.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Version 2 of the content onboarding, independent of the v1 service. It receives content already mapped to iGOT
 * format (the Deep Linking return API applies the partner's own Jolt spec), generic rules complete whatever is
 * mandatory, and the course is then saved as a draft or published into the same tables, cache and indexes the v1
 * flow uses.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CiosContentServiceImplV2 implements CiosContentServiceV2 {

    private final ContentPartnerService contentPartnerService;
    private final LtiContentFieldsHelper ltiContentFieldsHelper;
    private final CoursePublisher coursePublisher;
    private final PartnerCourseCountUpdater countUpdater;
    private final ObjectMapper objectMapper;

    @Override
    public ApiResponse onboardContent(OnboardContentV2Request request) {
        ApiResponse response = ProjectUtil.createDefaultResponse(Constants.ONBOARD_V2_API_ID);
        log.info("CiosContentServiceImplV2::onboardContent: request for partner {}, status {}",
                request.getPartnerCode(), request.getStatus());
        try {
            validateRequest(request);
            String status = statusInLowerCase(request.getStatus());
            JsonNode partner = loadPartnerByCode(request.getPartnerCode());
            log.debug("CiosContentServiceImplV2::onboardContent: partner {} loaded, id {}",
                    request.getPartnerCode(), partner.path(Constants.ID).asText());
            validateSentContentPartner(request, partner);
            ObjectNode content = contentFromRequest(request);
            applyFieldsSentByRequest(content, request);
            ltiContentFieldsHelper.rejectMissingMandatoryFields(content);
            List<String> defaultedFields = ltiContentFieldsHelper.addDefaultsForMissingFields(content, partner.path(Constants.DATA));
            log.info("CiosContentServiceImplV2::onboardContent: externalId {} completed with defaults for {}",
                    content.path(Constants.EXTERNAL_ID).asText(), defaultedFields);
            Timestamp now = new Timestamp(System.currentTimeMillis());
            ltiContentFieldsHelper.assignPlatformFields(content, request, partner, status, now);
            saveAsDraftOrLive(content, partner, status, now);
            countUpdater.updateCourseCounts(request.getPartnerCode());
            response.setResult(buildSuccessResult(status));
            log.info("CiosContentServiceImplV2::onboardContent: externalId {} saved as {} with contentId {}",
                    content.path(Constants.EXTERNAL_ID).asText(), status, content.path(Constants.CONTENT_ID).asText());
        } catch (CustomException e) {
            log.warn("CiosContentServiceImplV2::onboardContent: partner {} rejected: {}",
                    request.getPartnerCode(), e.getMessage());
            ProjectUtil.errorResponse(response, e.getMessage(),
                    e.getHttpStatusCode() != null ? e.getHttpStatusCode() : HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            log.error("CiosContentServiceImplV2::onboardContent: failed", e);
            ProjectUtil.errorResponse(response, Constants.ONBOARD_FAILED + ": " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    private void validateRequest(OnboardContentV2Request request) {
        if (StringUtils.isBlank(request.getPartnerCode())) {
            throw badRequestException(Constants.PARTNER_CODE_MISSING);
        }
        if (request.getContentData() == null || !request.getContentData().isObject()) {
            throw badRequestException(Constants.CONTENT_DATA_INVALID);
        }
    }

    /**
     * The stored status is lower case; clients may send any case.
     */
    private String statusInLowerCase(String status) {
        String normalized = StringUtils.lowerCase(StringUtils.trim(status));
        if (!Constants.DRAFT.equals(normalized) && !isLive(normalized)) {
            throw badRequestException(Constants.STATUS_NOT_VALID);
        }
        return normalized;
    }

    private static boolean isLive(String status) {
        return Constants.LIVE_STATUS_LOWER_CASE.equals(status);
    }

    private JsonNode loadPartnerByCode(String partnerCode) {
        log.info("CiosContentServiceImplV2::loadPartnerByCode: reading partner {}", partnerCode);
        ApiResponse partnerResponse = contentPartnerService.getContentDetailsByPartnerCode(partnerCode);
        if (partnerResponse == null) {
            throw new CustomException(Constants.ERROR, Constants.PARTNER_READ_FAILED + partnerCode,
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (partnerResponse.getResponseCode() != HttpStatus.OK) {
            throw new CustomException(Constants.ERROR, Constants.PARTNER_NOT_FOUND + partnerCode,
                    HttpStatus.NOT_FOUND);
        }
        return objectMapper.convertValue(partnerResponse.getResult(), JsonNode.class);
    }

    /**
     * A contentPartner sent by the client is kept as sent, so it must identify the partner the course is saved under.
     */
    private void validateSentContentPartner(OnboardContentV2Request request, JsonNode partner) {
        if (!request.hasContentPartner()) {
            return;
        }
        JsonNode sent = request.getContentPartner();
        if (!sent.isObject() || StringUtils.isAnyBlank(sent.path(Constants.ID).asText(),
                sent.path(Constants.PARTNERCODE).asText())) {
            throw badRequestException(Constants.CONTENT_PARTNER_INCOMPLETE);
        }
        if (!sent.path(Constants.ID).asText().equals(partner.path(Constants.ID).asText())) {
            throw badRequestException(Constants.CONTENT_PARTNER_MISMATCH + request.getPartnerCode());
        }
    }

    /**
     * The content API 2 already mapped to iGOT format, copied so the request itself is never changed.
     */
    private ObjectNode contentFromRequest(OnboardContentV2Request request) {
        JsonNode content = request.getContentData().path(Constants.CONTENT);
        if (!content.isObject()) {
            throw badRequestException(Constants.NO_CONTENT_OBJECT);
        }
        return ((ObjectNode) content).deepCopy();
    }

    /**
     * What the request sends wins over anything the mapped content already carries. It is applied first, so the
     * mandatory-field check and the defaults both see the same values the course will be saved with.
     */
    private void applyFieldsSentByRequest(ObjectNode content, OnboardContentV2Request request) {
        if (StringUtils.isNotBlank(request.getCourseType())) {
            content.set(Constants.COURSE_TYPE, TextNode.valueOf(request.getCourseType()));
        }
        if (request.getCourseEnrolLimit() != null) {
            content.set(Constants.COURSE_ENROL_LIMIT, IntNode.valueOf(request.getCourseEnrolLimit()));
        }
        if (request.getRequiredKarmaPoints() != null) {
            content.set(Constants.REQUIRED_KARMA_POINTS, IntNode.valueOf(request.getRequiredKarmaPoints()));
        }
        setWhenSent(content, Constants.COMPETENCIES_V5, request.getCompetenciesV5());
        setWhenSent(content, Constants.COMPETENCIES_V6, request.getCompetenciesV6());
        setWhenSent(content, Constants.BADGE_DETAILS_V1, request.getBadgeDetailsV1());
        if (StringUtils.isNotBlank(request.getDifficultyLevel())) {
            content.set(Constants.DIFFICULTY_LEVEL, TextNode.valueOf(request.getDifficultyLevel()));
        }
    }

    private static void setWhenSent(ObjectNode content, String field, JsonNode value) {
        if (value != null && !value.isNull()) {
            content.set(field, value);
        }
    }

    /**
     * Saves the course as a draft, or publishes it, according to the status the client asked for.
     */
    private void saveAsDraftOrLive(ObjectNode content, JsonNode partner, String status, Timestamp now) {
        ObjectNode courseData = objectMapper.createObjectNode();
        courseData.set(Constants.CONTENT, content);
        if (isLive(status)) {
            coursePublisher.publishAsLive(courseData, partner.path(Constants.DATA), now);
        } else {
            coursePublisher.saveAsDraft(courseData, now);
        }
    }

    /**
     * The same result the v1 onboard API returns: one message with the number of published courses out of the courses
     * sent. A draft is not counted as published.
     */
    private Map<String, Object> buildSuccessResult(String status) {
        int publishedCount = isLive(status) ? 1 : 0;
        Map<String, Object> result = new HashMap<>();
        result.put(Constants.ONBOARD_RESULT_MESSAGE_KEY,
                String.format(Constants.COURSES_PUBLISHED_MESSAGE, publishedCount, 1));
        return result;
    }

    private static CustomException badRequestException(String message) {
        return new CustomException(Constants.ERROR, message, HttpStatus.BAD_REQUEST);
    }
}

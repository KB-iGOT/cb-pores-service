package com.igot.cb.cios.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.pores.exceptions.CustomException;
import com.igot.cb.pores.util.Constants;
import com.igot.cb.pores.util.PayloadValidation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

/**
 * Saves a course as a draft, or checks it and publishes it as a live course.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoursePublisher {

    private final PayloadValidation payloadValidation;
    private final PublishLicenceRules licenceRules;
    private final CourseDataWriter courseDataWriter;

    /**
     * A draft is inactive, has no publish time and is only sent to cios staging.
     */
    public void saveAsDraft(ObjectNode courseData, Timestamp now) {
        ObjectNode content = courseContent(courseData);
        content.put(Constants.IS_ACTIVE, Constants.ACTIVE_STATUS_FALSE);
        content.put(Constants.PUBLISHED_ON, Constants.DRAFT_PUBLISHED_ON);
        content.put(Constants.CREATED_DATE, now.toString());
        courseDataWriter.sendToCiosStaging(courseData);
        log.info("CoursePublisher::saveAsDraft: externalId {} sent to cios staging as a draft",
                content.path(Constants.EXTERNAL_ID).asText());
    }

    /**
     * Publishes the course: it must pass the live content schema and the partner's licence rules, and is then sent to
     * cios staging and saved in the live table, cache and index.
     *
     * @throws CustomException with status 400 and the reason when the course may not be published
     */
    public void publishAsLive(ObjectNode courseData, JsonNode partnerData, Timestamp now) {
        payloadValidation.validatePayload(Constants.CIOS_CONTENT_VALIDATION_FILE_JSON, courseData);
        ObjectNode content = courseContent(courseData);
        content.put(Constants.IS_ACTIVE, Constants.ACTIVE_STATUS);
        content.put(Constants.PUBLISHED_ON, now.toString());
        content.put(Constants.UPDATED_DATE, now.toString());
        licenceRules.findLicenceRuleViolation(content, partnerData).ifPresent(reason -> {
            log.warn("CoursePublisher::publishAsLive: externalId {} breaks a licence rule: {}",
                    content.path(Constants.EXTERNAL_ID).asText(), reason);
            throw new CustomException(Constants.ERROR, reason, HttpStatus.BAD_REQUEST);
        });
        courseDataWriter.sendToCiosStaging(courseData);
        courseDataWriter.saveInLiveTableCacheAndIndex(courseData, now);
        log.info("CoursePublisher::publishAsLive: externalId {} published as live",
                content.path(Constants.EXTERNAL_ID).asText());
    }

    private static ObjectNode courseContent(ObjectNode courseData) {
        return (ObjectNode) courseData.path(Constants.CONTENT);
    }
}

package com.igot.cb.cios.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.cios.entity.CiosContentEntity;
import com.igot.cb.cios.repository.CiosRepository;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Publish-time rules that depend on the partner's licence: course type, enrol limit and karma points.
 *
 * <ul>
 *   <li>User licence: the course type is removed, karma points are forced to 0 and the enrol limit must not exceed the
 *       partner's overall limit.</li>
 *   <li>Course licence: the course type of an onboarded course cannot change. A paid course gets its karma coins and
 *       compatibility level, its enrol limit must not exceed the overall limit and its karma points must reach the
 *       partner's karma points. A free course is not restricted further.</li>
 *   <li>No licence configured: nothing is applied.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class PublishLicenceRules {

    private final CiosRepository ciosRepository;
    private final CbServerProperties cbServerProperties;

    /**
     * Applies the rules to the content.
     *
     * @param content     the content about to be published; may be adjusted by the rules
     * @param partnerData the partner's {@code data} node
     * @return the reason the course must not be published, or empty when it may be
     */
    public Optional<String> findLicenceRuleViolation(ObjectNode content, JsonNode partnerData) {
        String licenceType = partnerData.path(Constants.LICENCE_TYPE).asText("");
        int overAllLimit = partnerData.path(Constants.OVER_ALL_LIMIT).asInt(0);
        if (Constants.LICENCE_TYPE_USER.equalsIgnoreCase(licenceType)) {
            return findUserLicenceViolation(content, overAllLimit);
        }
        if (Constants.LICENCE_TYPE_COURSE.equalsIgnoreCase(licenceType)) {
            return findCourseLicenceViolation(content, partnerData, overAllLimit);
        }
        return Optional.empty();
    }

    private Optional<String> findUserLicenceViolation(ObjectNode content, int overAllLimit) {
        content.remove(Constants.COURSE_TYPE);
        content.put(Constants.REQUIRED_KARMA_POINTS, 0);
        return findEnrolLimitViolation(content, overAllLimit);
    }

    private Optional<String> findCourseLicenceViolation(ObjectNode content, JsonNode partnerData, int overAllLimit) {
        if (courseTypeDiffersFromOnboardedCourse(content)) {
            return Optional.of(Constants.COURSE_TYPE_CHANGED);
        }
        if (!Constants.COURSE_TYPE_PAID.equalsIgnoreCase(content.path(Constants.COURSE_TYPE).asText())) {
            return Optional.empty();
        }
        setKarmaCoinsAndCompatibilityLevel(content, partnerData);
        Optional<String> enrolLimitViolation = findEnrolLimitViolation(content, overAllLimit);
        return enrolLimitViolation.isPresent()
                ? enrolLimitViolation
                : findKarmaPointsViolation(content, partnerData.path(Constants.KARMA_POINTS).asInt(0));
    }

    private Optional<String> findEnrolLimitViolation(ObjectNode content, int overAllLimit) {
        return content.path(Constants.COURSE_ENROL_LIMIT).asInt(0) > overAllLimit
                ? Optional.of(String.format(Constants.ENROL_LIMIT_EXCEEDED, overAllLimit))
                : Optional.empty();
    }

    private Optional<String> findKarmaPointsViolation(ObjectNode content, int partnerKarmaPoints) {
        return content.path(Constants.REQUIRED_KARMA_POINTS).asInt(0) < partnerKarmaPoints
                ? Optional.of(String.format(Constants.KARMA_BELOW_PARTNER, partnerKarmaPoints))
                : Optional.empty();
    }

    private void setKarmaCoinsAndCompatibilityLevel(ObjectNode content, JsonNode partnerData) {
        double multiplier = partnerData.path(Constants.KARMA_COIN_MULTIPLIER).asDouble(1);
        int karmaCoins = (int) Math.round(multiplier * content.path(Constants.REQUIRED_KARMA_POINTS).asInt(0));
        content.put(Constants.REQUIRED_KARMA_COINS, karmaCoins);
        content.put(Constants.COMPATIBILITY_LEVEL,
                karmaCoins > 0 ? cbServerProperties.getExtCourseCompatibilityLevel() : 0);
    }

    /**
     * True when the course is already onboarded with a different course type than the one requested now.
     */
    private boolean courseTypeDiffersFromOnboardedCourse(ObjectNode content) {
        String requested = content.path(Constants.COURSE_TYPE).asText("");
        return ciosRepository.findByExternalIdAndPartnerId(
                        content.path(Constants.EXTERNAL_ID).asText(),
                        content.path(Constants.CONTENT_PARTNER).path(Constants.ID).asText())
                .map(CiosContentEntity::getCiosData)
                .map(data -> data.path(Constants.CONTENT).path(Constants.COURSE_TYPE).asText(""))
                .filter(StringUtils::isNotBlank)
                .map(existing -> !existing.equalsIgnoreCase(requested))
                .orElse(false);
    }
}

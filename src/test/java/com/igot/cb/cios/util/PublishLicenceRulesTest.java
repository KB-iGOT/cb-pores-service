package com.igot.cb.cios.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.cios.entity.CiosContentEntity;
import com.igot.cb.cios.repository.CiosRepository;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishLicenceRulesTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private CiosRepository ciosRepository;
    @Mock
    private CbServerProperties cbServerProperties;

    private PublishLicenceRules rules() {
        return new PublishLicenceRules(ciosRepository, cbServerProperties);
    }

    private ObjectNode content(String courseType, int enrolLimit, int karmaPoints) {
        ObjectNode content = mapper.createObjectNode();
        content.put("externalId", "ext-1");
        content.putObject("contentPartner").put("id", "partner-1");
        content.put("courseType", courseType);
        content.put("courseEnrolLimit", enrolLimit);
        content.put("requiredKarmaPoints", karmaPoints);
        return content;
    }

    private JsonNode partner(String json) throws Exception {
        return mapper.readTree(json);
    }

    private void alreadyOnboardedAs(String courseType) {
        CiosContentEntity entity = new CiosContentEntity();
        entity.setCiosData(mapper.createObjectNode().set("content", mapper.createObjectNode().put("courseType", courseType)));
        when(ciosRepository.findByExternalIdAndPartnerId("ext-1", "partner-1")).thenReturn(Optional.of(entity));
    }

    @Test
    void partnerWithoutALicence_allowsEverythingAndChangesNothing() throws Exception {
        ObjectNode content = content("paid", 500, 0);

        Optional<String> violation = rules().findLicenceRuleViolation(content, partner("{\"overAllLimit\":1}"));

        assertTrue(violation.isEmpty());
        assertEquals(500, content.path("courseEnrolLimit").asInt());
        assertEquals("paid", content.path("courseType").asText());
        verifyNoInteractions(ciosRepository);
    }

    @Test
    void userLicence_removesTheCourseTypeAndZeroesKarma() throws Exception {
        ObjectNode content = content("paid", 5, 40);

        Optional<String> violation = rules().findLicenceRuleViolation(content,
                partner("{\"licenseType\":\"User\",\"overAllLimit\":10}"));

        assertTrue(violation.isEmpty());
        assertFalse(content.has("courseType"));
        assertEquals(0, content.path("requiredKarmaPoints").asInt());
    }

    @Test
    void userLicence_enrolLimitAboveTheOverallLimitIsRefused() throws Exception {
        Optional<String> violation = rules().findLicenceRuleViolation(content("paid", 11, 0),
                partner("{\"licenseType\":\"user\",\"overAllLimit\":10}"));

        assertEquals(String.format(Constants.ENROL_LIMIT_EXCEEDED, 10), violation.orElseThrow());
    }

    @Test
    void courseLicence_freeCourseIsNotRestrictedFurther() throws Exception {
        ObjectNode content = content("free", 999, 0);

        Optional<String> violation = rules().findLicenceRuleViolation(content,
                partner("{\"licenseType\":\"Course\",\"overAllLimit\":10,\"karmaPoints\":50}"));

        assertTrue(violation.isEmpty());
        assertFalse(content.has("requiredKarmaCoins"));
    }

    @Test
    void courseLicence_paidCourseGetsKarmaCoinsAndTheCompatibilityLevel() throws Exception {
        when(cbServerProperties.getExtCourseCompatibilityLevel()).thenReturn(7);
        ObjectNode content = content("paid", 5, 10);

        Optional<String> violation = rules().findLicenceRuleViolation(content,
                partner("{\"licenseType\":\"Course\",\"overAllLimit\":10,\"karmaPoints\":10,\"karmaCoinMultiplier\":1.5}"));

        assertTrue(violation.isEmpty());
        assertEquals(15, content.path("requiredKarmaCoins").asInt());
        assertEquals(7, content.path("compatibilityLevel").asInt());
    }

    @Test
    void courseLicence_paidCourseWithoutKarmaGetsNoCoinsAndCompatibilityLevelZero() throws Exception {
        ObjectNode content = content("paid", 0, 0);

        Optional<String> violation = rules().findLicenceRuleViolation(content,
                partner("{\"licenseType\":\"Course\",\"overAllLimit\":10}"));

        assertTrue(violation.isEmpty());
        assertEquals(0, content.path("requiredKarmaCoins").asInt());
        assertEquals(0, content.path("compatibilityLevel").asInt());
    }

    @Test
    void courseLicence_paidEnrolLimitAboveTheOverallLimitIsRefused() throws Exception {
        Optional<String> violation = rules().findLicenceRuleViolation(content("paid", 11, 10),
                partner("{\"licenseType\":\"Course\",\"overAllLimit\":10,\"karmaPoints\":10}"));

        assertEquals(String.format(Constants.ENROL_LIMIT_EXCEEDED, 10), violation.orElseThrow());
    }

    @Test
    void courseLicence_paidKarmaBelowThePartnersKarmaIsRefused() throws Exception {
        Optional<String> violation = rules().findLicenceRuleViolation(content("paid", 5, 9),
                partner("{\"licenseType\":\"Course\",\"overAllLimit\":10,\"karmaPoints\":10}"));

        assertEquals(String.format(Constants.KARMA_BELOW_PARTNER, 10), violation.orElseThrow());
    }

    @Test
    void courseLicence_changingTheCourseTypeOfAnOnboardedCourseIsRefused() throws Exception {
        alreadyOnboardedAs("free");

        Optional<String> violation = rules().findLicenceRuleViolation(content("paid", 0, 0),
                partner("{\"licenseType\":\"Course\",\"overAllLimit\":10}"));

        assertEquals(Constants.COURSE_TYPE_CHANGED, violation.orElseThrow());
    }

    @Test
    void courseLicence_republishingWithTheSameCourseTypeIsAllowed() throws Exception {
        alreadyOnboardedAs("PAID");

        Optional<String> violation = rules().findLicenceRuleViolation(content("paid", 0, 0),
                partner("{\"licenseType\":\"Course\",\"overAllLimit\":10}"));

        assertTrue(violation.isEmpty());
    }

    @Test
    void courseLicence_moreThanOneLiveRowForTheCourse_failsWithTheSameExceptionAfterLoggingIt() throws Exception {
        IncorrectResultSizeDataAccessException duplicateRows = new IncorrectResultSizeDataAccessException(1, 2);
        when(ciosRepository.findByExternalIdAndPartnerId("ext-1", "partner-1")).thenThrow(duplicateRows);
        ObjectNode content = content("paid", 0, 0);
        JsonNode partner = partner("{\"licenseType\":\"Course\",\"overAllLimit\":10}");

        PublishLicenceRules rules = rules();
        IncorrectResultSizeDataAccessException thrown = assertThrows(IncorrectResultSizeDataAccessException.class,
                () -> rules.findLicenceRuleViolation(content, partner));

        assertSame(duplicateRows, thrown);
    }
}

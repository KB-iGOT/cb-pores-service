package com.igot.cb.cios.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.pores.exceptions.CustomException;
import com.igot.cb.pores.util.Constants;
import com.igot.cb.pores.util.PayloadValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.sql.Timestamp;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoursePublisherTest {

    private static final Timestamp NOW = Timestamp.valueOf("2026-11-22 10:15:30");

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private PayloadValidation payloadValidation;
    @Mock
    private PublishLicenceRules licenceRules;
    @Mock
    private CourseDataWriter courseDataWriter;

    private CoursePublisher publisher;
    private ObjectNode courseData;
    private final JsonNode partnerData = new ObjectMapper().createObjectNode();

    @BeforeEach
    void setUp() {
        publisher = new CoursePublisher(payloadValidation, licenceRules, courseDataWriter);
        courseData = mapper.createObjectNode();
        courseData.putObject("content").put("name", "Course");
    }

    private ObjectNode content() {
        return (ObjectNode) courseData.path("content");
    }

    @Test
    void draft_isInactiveHasNoPublishTimeAndIsOnlyStaged() {
        publisher.saveAsDraft(courseData, NOW);

        assertFalse(content().path("isActive").asBoolean(true));
        assertEquals(Constants.DRAFT_PUBLISHED_ON, content().path("publishedOn").asText());
        assertEquals(NOW.toString(), content().path("createdDate").asText());
        verify(courseDataWriter).sendToCiosStaging(courseData);
        verify(courseDataWriter, never()).saveInLiveTableCacheAndIndex(any(), any());
        verifyNoInteractions(payloadValidation, licenceRules);
    }

    @Test
    void live_isValidatedChecked_thenStagedAndSavedInThatOrder() {
        when(licenceRules.findLicenceRuleViolation(any(), any())).thenReturn(Optional.empty());

        publisher.publishAsLive(courseData, partnerData, NOW);

        assertTrue(content().path("isActive").asBoolean());
        assertEquals(NOW.toString(), content().path("publishedOn").asText());
        assertEquals(NOW.toString(), content().path("updatedDate").asText());
        InOrder order = inOrder(payloadValidation, licenceRules, courseDataWriter);
        order.verify(payloadValidation).validatePayload(Constants.CIOS_CONTENT_VALIDATION_FILE_JSON, courseData);
        order.verify(licenceRules).findLicenceRuleViolation(content(), partnerData);
        order.verify(courseDataWriter).sendToCiosStaging(courseData);
        order.verify(courseDataWriter).saveInLiveTableCacheAndIndex(courseData, NOW);
    }

    @Test
    void live_thatFailsTheContentSchema_isNeverStored() {
        doThrow(new CustomException("Validation Error", "name is missing", HttpStatus.BAD_REQUEST))
                .when(payloadValidation).validatePayload(any(), any());

        CustomException e = assertThrows(CustomException.class, () -> publisher.publishAsLive(courseData, partnerData, NOW));

        assertEquals("name is missing", e.getMessage());
        verifyNoInteractions(licenceRules, courseDataWriter);
    }

    @Test
    void live_thatBreaksALicenceRule_isABadRequestWithTheReasonAndNeverStored() {
        when(licenceRules.findLicenceRuleViolation(any(), any())).thenReturn(Optional.of("karma too low"));

        CustomException e = assertThrows(CustomException.class, () -> publisher.publishAsLive(courseData, partnerData, NOW));

        assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatusCode());
        assertEquals("karma too low", e.getMessage());
        verifyNoInteractions(courseDataWriter);
    }
}

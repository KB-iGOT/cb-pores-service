package com.igot.cb.contentpartner.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;

class ContentPartnerEntityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testContentPartnerEntity_GettersAndSetters() throws Exception {
        ContentPartnerEntity entity = new ContentPartnerEntity();

        String id = "partner-001";
        JsonNode data = objectMapper.readTree("{\"key\":\"value\"}");
        Timestamp now = new Timestamp(System.currentTimeMillis());

        entity.setId(id);
        entity.setData(data);
        entity.setCreatedOn(now);
        entity.setUpdatedOn(now);
        entity.setIsActive(true);
        entity.setTrasformContentJson(data);
        entity.setTransformContentViaApi(data);
        entity.setTransformProgressJson(data);
        entity.setTransformProgressViaApi(data);
        entity.setCertificateTemplateUrl("http://template.url");
        entity.setServiceRegistryDetails(data);
        entity.setContentFileValidation(data);

        assertEquals(id, entity.getId());
        assertEquals(data, entity.getData());
        assertEquals(now, entity.getCreatedOn());
        assertEquals(now, entity.getUpdatedOn());
        assertTrue(entity.getIsActive());
        assertEquals(data, entity.getTrasformContentJson());
        assertEquals(data, entity.getTransformContentViaApi());
        assertEquals(data, entity.getTransformProgressJson());
        assertEquals(data, entity.getTransformProgressViaApi());
        assertEquals("http://template.url", entity.getCertificateTemplateUrl());
        assertEquals(data, entity.getServiceRegistryDetails());
        assertEquals(data, entity.getContentFileValidation());
    }

    @Test
    void testAllArgsConstructor() throws Exception {
        JsonNode data = objectMapper.readTree("{\"sample\":\"test\"}");
        Timestamp ts = new Timestamp(System.currentTimeMillis());

        ContentPartnerEntity entity = new ContentPartnerEntity(
                "id-123",
                data,
                ts,
                ts,
                false,
                data,
                data,
                data,
                data,
                "http://url.com",
                data,
                data
        );

        assertEquals("id-123", entity.getId());
        assertEquals(data, entity.getData());
        assertEquals(ts, entity.getCreatedOn());
        assertEquals(ts, entity.getUpdatedOn());
        assertFalse(entity.getIsActive());
        assertEquals("http://url.com", entity.getCertificateTemplateUrl());
    }
}

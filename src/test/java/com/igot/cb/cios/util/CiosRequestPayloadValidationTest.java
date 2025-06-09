package com.igot.cb.cios.util;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.igot.cb.cios.dto.ObjectDto;
import com.igot.cb.pores.exceptions.CustomException;
import org.apache.htrace.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

public class CiosRequestPayloadValidationTest {

    private CiosRequestPayloadValidation validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        validator = new CiosRequestPayloadValidation();
        objectMapper = new ObjectMapper();
    }


    @Test
    void testValidateModel_whenStatusIsNull_shouldThrowException() {
        ObjectDto dto = new ObjectDto();
        dto.setStatus(null);  // Missing status

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateModel(dto));
        assertEquals("Status is missing in the request", ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatusCode());
    }

    @Test
    void testValidateModel_whenContentDataIsNull_shouldThrowException() {
        ObjectDto dto = new ObjectDto();
        dto.setStatus("APPROVED");
        dto.setContentData(null);  // Missing content data
       // dto.setContentPartner(objectMapper.createObjectNode());

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateModel(dto));
        assertEquals("Content Data is missing in the request", ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatusCode());
    }

    @Test
    void testValidateModel_whenContentPartnerIsNull_shouldThrowException() {
        ObjectDto dto = new ObjectDto();
        dto.setStatus("APPROVED");
        //dto.setContentData(objectMapper.createObjectNode());
        dto.setContentPartner(null);  // Missing content partner

        CustomException ex = assertThrows(CustomException.class, () -> validator.validateModel(dto));
        assertEquals("Content Data is missing in the request", ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatusCode());
    }
}
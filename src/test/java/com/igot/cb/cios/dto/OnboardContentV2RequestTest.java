package com.igot.cb.cios.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OnboardContentV2RequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void aTopLevelPartnerCode_wins() throws Exception {
        OnboardContentV2Request request = new OnboardContentV2Request();
        request.setPartnerCode("TOP");
        request.setContentPartner(mapper.readTree("{\"id\":\"p\",\"partnerCode\":\"INSIDE\"}"));

        assertEquals("TOP", request.getPartnerCode());
    }

    @Test
    void withoutATopLevelPartnerCode_itComesFromContentPartnerLikeInTheV1Request() throws Exception {
        OnboardContentV2Request request = new OnboardContentV2Request();
        request.setPartnerCode("  ");
        request.setContentPartner(mapper.readTree("{\"id\":\"p\",\"partnerCode\":\"INSIDE\"}"));

        assertEquals("INSIDE", request.getPartnerCode());
    }

    @Test
    void withNeither_theCodeIsNull() throws Exception {
        OnboardContentV2Request none = new OnboardContentV2Request();
        OnboardContentV2Request withoutCode = new OnboardContentV2Request();
        withoutCode.setContentPartner(mapper.readTree("{\"id\":\"p\"}"));
        OnboardContentV2Request jsonNull = new OnboardContentV2Request();
        jsonNull.setContentPartner(mapper.nullNode());

        assertNull(none.getPartnerCode());
        assertNull(withoutCode.getPartnerCode());
        assertNull(jsonNull.getPartnerCode());
    }
}

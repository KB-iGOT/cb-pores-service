package com.igot.cb.demand.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class StatusTransitionConfigTest {

    @Test
    void testValidTransition() throws IOException {
        StatusTransitionConfig config = new StatusTransitionConfig("/payloadValidation/statusTransitions.json");

        // Invalid transitions
        assertFalse(config.isValidTransition("contentRequest", "draft", "approved")); // not allowed
        assertFalse(config.isValidTransition("contentRequest", "published", "draft")); // no such current status
        assertFalse(config.isValidTransition("nonExistingType", "draft", "review")); // no such requestType
    }
}

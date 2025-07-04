package com.igot.cb.transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igot.cb.demandinterest.service.KarmaQuestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KarmaQuestControllerTest {

    @InjectMocks
    private KarmaQuestController controller;

    @Mock
    private KarmaQuestServiceImpl serviceClass;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
    }

    @Test
    void testProcessInterests() {
        String interestId = "123";
        String expectedResponse = "some interest data";

        when(serviceClass.getInterest(interestId)).thenReturn(expectedResponse);

        Object actual = controller.processInterests(interestId);

        assertEquals(expectedResponse, actual);
        verify(serviceClass, times(1)).getInterest(interestId);
    }

    @Test
    void testCreateInterest() throws Exception {
        String jsonString = "{\"key\":\"value\"}";
        JsonNode jsonNode = objectMapper.readTree(jsonString);
        String expectedResponse = "insert success";

        when(serviceClass.insertInterest(jsonNode)).thenReturn(expectedResponse);

        Object actual = controller.createInterest(jsonNode);

        assertEquals(expectedResponse, actual);
        verify(serviceClass, times(1)).insertInterest(jsonNode);
    }
}

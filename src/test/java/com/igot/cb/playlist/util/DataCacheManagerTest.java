package com.igot.cb.playlist.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DataCacheManagerTest {

    private DataCacheManager dataCacheManager;

    @BeforeEach
    void setUp() {
        dataCacheManager = new DataCacheManager();
    }

    @Test
    void testPutContentInCacheAndGetContentFromCache_Hit() {
        String key = "testKey";
        Map<String, Object> value = new HashMap<>();
        value.put("title", "Java Playlist");

        dataCacheManager.putContentInCache(key, value);

        Map<String, Object> cachedValue = dataCacheManager.getContentFromCache(key);

        assertNotNull(cachedValue);
        assertEquals("Java Playlist", cachedValue.get("title"));
    }

    @Test
    void testGetContentFromCache_Miss() {
        Map<String, Object> result = dataCacheManager.getContentFromCache("nonExistingKey");
        assertNull(result);
    }
}

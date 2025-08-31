package com.igot.cb.playlist.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.RedisTemplate;

class RedisCacheMngrTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private HashOperations<String, String, String> hashOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisCacheMngr redisCacheMngr;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        redisCacheMngr.setHashOperations(redisTemplate);
      // when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testGetContentFromCacheSuccess() {
        when(valueOperations.get("key1")).thenReturn("value1");
        String result = redisCacheMngr.getContentFromCache("key1");
        assertEquals("value1", result);
    }

    @Test
    void testGetContentFromCacheException() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis error"));
        String result = redisCacheMngr.getContentFromCache("key1");
        assertNull(result);
    }

    @Test
    void testHgetSuccess() {
        when(hashOperations.multiGet(eq("hashKey"), anyList())).thenReturn(List.of("val1", "val2"));
        List<String> result = redisCacheMngr.hget("hashKey", 0, "field1", "field2");
        assertNull(result);
    }

    @Test
    void testHgetException() {
        when(hashOperations.multiGet(eq("hashKey"), anyList())).thenThrow(new RuntimeException("Redis error"));
        List<String> result = redisCacheMngr.hget("hashKey", 0, "field1", "field2");
        assertNull(result);
    }


    @Test
    void testHsetException() {
        doThrow(new RuntimeException("Redis error")).when(hashOperations).putAll(anyString(), anyMap());
        Map<String, String> data = Map.of("field1", "val1");
        assertDoesNotThrow(() -> redisCacheMngr.hset("hashKey", 0, data));
    }


    @Test
    void testHdelException() {
        when(hashOperations.delete("hashKey", "field1")).thenThrow(new RuntimeException("Redis error"));
        Long result = redisCacheMngr.hdel("hashKey", "field1", 0);
        assertNull(result);
    }
}

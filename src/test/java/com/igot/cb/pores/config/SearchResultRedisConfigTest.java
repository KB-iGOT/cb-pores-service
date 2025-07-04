package com.igot.cb.pores.config;

import com.igot.cb.pores.elasticsearch.dto.SearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SearchResultRedisConfigTest {

    @Test
    void testRedisTemplateForSearchResult() {
        // Arrange
        RedisConnectionFactory mockConnectionFactory = mock(RedisConnectionFactory.class);
        SearchResultRedisConfig config = new SearchResultRedisConfig();

        // Act
        RedisTemplate<String, SearchResult> redisTemplate =
                config.redisTemplateForSearchResult(mockConnectionFactory);

        // Assert
        assertNotNull(redisTemplate);
        assertEquals(StringRedisSerializer.class, redisTemplate.getKeySerializer().getClass());
        assertEquals(mockConnectionFactory, redisTemplate.getConnectionFactory());
    }
}


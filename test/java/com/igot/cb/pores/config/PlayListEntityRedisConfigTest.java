package com.igot.cb.pores.config;

import com.igot.cb.playlist.entity.PlayListEntity;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayListEntityRedisConfigTest {

    @Test
    void testRedisTemplateForPlayListEntity() {
        // Arrange
        RedisConnectionFactory mockConnectionFactory = mock(RedisConnectionFactory.class);
        PlayListEntityRedisConfig config = new PlayListEntityRedisConfig();

        // Act
        RedisTemplate<String, PlayListEntity> redisTemplate =
                config.redisTemplateForPlayListEntity(mockConnectionFactory);

        // Assert
        assertNotNull(redisTemplate);
        assertEquals(StringRedisSerializer.class, redisTemplate.getKeySerializer().getClass());
        assertEquals(mockConnectionFactory, redisTemplate.getConnectionFactory());
    }
}

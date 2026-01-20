package com.igot.cb.pores.config;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class RedisConfigTest {

    private RedisConfig redisConfig;

    @BeforeEach
    void setUp() {
        redisConfig = new RedisConfig();
        // Inject test properties using ReflectionTestUtils
        ReflectionTestUtils.setField(redisConfig, "redisHost", "localhost");
        ReflectionTestUtils.setField(redisConfig, "redisPort", 6379);
    }

    @Test
    void testRedisConnectionFactory_ShouldReturnLettuceConnectionFactory() {
        // When
        RedisConnectionFactory factory = redisConfig.redisConnectionFactory();

        // Then
        assertNotNull(factory, "RedisConnectionFactory should not be null");
        assertTrue(factory instanceof LettuceConnectionFactory, "Should be LettuceConnectionFactory");

        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) factory;
        assertEquals("localhost", lettuceFactory.getHostName(), "Host should be localhost");
        assertEquals(6379, lettuceFactory.getPort(), "Port should be 6379");
        assertEquals(0, lettuceFactory.getDatabase(), "Database should be 0");
    }

    @Test
    void testRedisTemplate_ShouldBeConfiguredCorrectly() {
        // Given
        RedisConnectionFactory factory = redisConfig.redisConnectionFactory();

        // When
        RedisTemplate<String, String> template = redisConfig.redisTemplate(factory);

        // Then
        assertNotNull(template, "RedisTemplate should not be null");
        assertNotNull(template.getConnectionFactory(), "ConnectionFactory should be set");
        assertNotNull(template.getKeySerializer(), "Key serializer should be set");
        assertNotNull(template.getValueSerializer(), "Value serializer should be set");
        assertNotNull(template.getHashKeySerializer(), "Hash key serializer should be set");
        assertNotNull(template.getHashValueSerializer(), "Hash value serializer should be set");
    }

    @Test
    void testRedisTemplate_WithNullFactory_ShouldNotFail() {
        // When
        RedisTemplate<String, String> template = redisConfig.redisTemplate(null);

        // Then
        assertNotNull(template, "RedisTemplate should be created even with null factory");
        assertNull(template.getConnectionFactory(), "ConnectionFactory should be null");
    }

    @Test
    void testBuildPoolConfig_ShouldHaveCorrectSettings() throws Exception {
        // Use reflection to access the private buildPoolConfig method
        java.lang.reflect.Method method = RedisConfig.class.getDeclaredMethod("buildPoolConfig");
        method.setAccessible(true);

        // When
        GenericObjectPoolConfig<?> poolConfig = (GenericObjectPoolConfig<?>) method.invoke(redisConfig);

        // Then
        assertNotNull(poolConfig, "Pool config should not be null");
        assertEquals(3000, poolConfig.getMaxTotal(), "MaxTotal should be 3000");
        assertEquals(128, poolConfig.getMaxIdle(), "MaxIdle should be 128");
        assertEquals(100, poolConfig.getMinIdle(), "MinIdle should be 100");
        assertEquals(5000, poolConfig.getMaxWaitDuration().toMillis(), "MaxWait should be 5000ms");
    }

    @Test
    void testRedisConnectionFactory_WithDifferentPort() {
        // Given - Change port to test different configuration
        ReflectionTestUtils.setField(redisConfig, "redisPort", 6380);

        // When
        RedisConnectionFactory factory = redisConfig.redisConnectionFactory();

        // Then
        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) factory;
        assertEquals(6380, lettuceFactory.getPort(), "Port should be 6380");
    }

    @Test
    void testRedisConnectionFactory_WithDifferentHost() {
        // Given
        ReflectionTestUtils.setField(redisConfig, "redisHost", "redis-server");

        // When
        RedisConnectionFactory factory = redisConfig.redisConnectionFactory();

        // Then
        LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) factory;
        assertEquals("redis-server", lettuceFactory.getHostName(), "Host should be redis-server");
    }
}

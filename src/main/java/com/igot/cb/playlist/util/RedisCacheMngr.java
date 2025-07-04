package com.igot.cb.playlist.util;

import com.igot.cb.pores.config.RedisConfig;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

@Component
@Slf4j
public class RedisCacheMngr {

  @Autowired
  private RedisConfig redisConfig;

  private RedisTemplate<String, String> redisTemplate;
  private HashOperations<String, String, String> hashOperations;
  @Autowired
  public void setHashOperations(RedisTemplate<String, String> redisTemplate) {
    this.hashOperations = redisTemplate.opsForHash();
  }

  public String getContentFromCache(String key) {
    try {
      return redisTemplate.opsForValue().get(key);
    } catch (Exception e) {
      log.error("Error getting content from cache: {}", e.getMessage());
      return null;
    }
  }

  public List<String> hget(String key, int index, String... fields) {
    try {
      return hashOperations.multiGet(key, List.of(fields)).stream()
              .collect(Collectors.toList());
    } catch (Exception e) {
      log.error("Error getting hash fields from cache: {}", e.getMessage());
      return null;
    }
  }

  public void hset(String key, int index, Map<String, String> fieldValues) {
    try {
      hashOperations.putAll(key, fieldValues);
    } catch (Exception e) {
      log.error("Error setting hash fields in cache: {}", e.getMessage());
    }
  }

  // Method to delete a field from a hash
  public Long hdel(String key, String field, int index) {
    try {
      Long result = hashOperations.delete(key, field);
      if (result == 1) {
        log.info("Field {} deleted successfully from key {}.", field, key);
      } else {
        log.warn("Field {} not found in key {}.", field, key);
      }
      return result;
    } catch (Exception e) {
      log.error("Error deleting hash field from cache: {}", e.getMessage());
      return null;
    }
  }

}

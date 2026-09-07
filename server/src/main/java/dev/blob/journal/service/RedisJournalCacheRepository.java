package dev.blob.journal.service;

import dev.blob.journal.dto.JournalDetailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

@Repository
public class RedisJournalCacheRepository implements JournalCacheRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisJournalCacheRepository.class);
    private static final String KEY_PREFIX = "blob:cache:journal:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisJournalCacheRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<JournalDetailResponse> get(long id) {
        try {
            String json = redisTemplate.opsForValue().get(key(id));
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, JournalDetailResponse.class));
        } catch (DataAccessException exception) {
            LOGGER.warn("Redis journal cache read unavailable");
            return Optional.empty();
        } catch (JacksonException exception) {
            throw new IllegalStateException("Invalid journal cache payload", exception);
        }
    }

    @Override
    public void put(JournalDetailResponse journal) {
        try {
            redisTemplate.opsForValue().set(key(journal.id()), objectMapper.writeValueAsString(journal), TTL);
        } catch (DataAccessException exception) {
            LOGGER.warn("Redis journal cache write unavailable");
        } catch (JacksonException exception) {
            throw new IllegalStateException("Unable to serialize journal cache payload", exception);
        }
    }

    @Override
    public void evict(long id) {
        try {
            redisTemplate.delete(key(id));
        } catch (DataAccessException exception) {
            LOGGER.warn("Redis journal cache eviction unavailable");
        }
    }

    private String key(long id) {
        return KEY_PREFIX + id;
    }
}

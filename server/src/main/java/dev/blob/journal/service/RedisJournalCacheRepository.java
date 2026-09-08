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
import java.util.UUID;

@Repository
public class RedisJournalCacheRepository implements JournalCacheRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisJournalCacheRepository.class);
    private static final String KEY_PREFIX = "blob:cache:journal:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    // A new process never trusts cache left by a prior run or database configuration.
    private volatile String generation = UUID.randomUUID().toString();

    public RedisJournalCacheRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<JournalDetailResponse> get(long id) {
        String observedGeneration = generation;
        try {
            String json = redisTemplate.opsForValue().get(key(observedGeneration, id));
            if (json == null) {
                return Optional.empty();
            }
            JournalDetailResponse detail = objectMapper.readValue(json, JournalDetailResponse.class);
            return observedGeneration.equals(generation) ? Optional.ofNullable(detail) : Optional.empty();
        } catch (DataAccessException exception) {
            LOGGER.warn("Redis journal cache read unavailable");
            return Optional.empty();
        } catch (JacksonException exception) {
            LOGGER.warn("Invalid journal cache payload; using MySQL");
            evict(id);
            return Optional.empty();
        }
    }

    @Override
    public void put(JournalDetailResponse journal) {
        try {
            redisTemplate.opsForValue().set(key(generation, journal.id()), objectMapper.writeValueAsString(journal), TTL);
        } catch (DataAccessException exception) {
            LOGGER.warn("Redis journal cache write unavailable");
        } catch (JacksonException exception) {
            LOGGER.warn("Journal cache serialization unavailable");
        }
    }

    @Override
    public void evict(long id) {
        try {
            redisTemplate.delete(key(generation, id));
        } catch (DataAccessException exception) {
            // Uncertain invalidation means old entries must stay unreachable after recovery.
            // Rotate only our cache namespace; abandoned keys expire without a Redis-wide flush.
            generation = UUID.randomUUID().toString();
            LOGGER.warn("Redis journal cache eviction unavailable; rebuilding journal cache generation");
        }
    }

    private String key(String cacheGeneration, long id) {
        return KEY_PREFIX + cacheGeneration + ":" + id;
    }
}

package dev.blob.journal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.util.List;
import java.util.Set;

@Repository
public class RedisSearchHistoryRepository implements SearchHistoryRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisSearchHistoryRepository.class);
    private static final String RECENT_KEY = "blob:search:recent";
    private static final String FREQUENCY_KEY = "blob:search:frequency";
    private static final long MAX_RECENT_TERMS = 50;

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;

    public RedisSearchHistoryRepository(StringRedisTemplate redisTemplate, Clock clock) {
        this.redisTemplate = redisTemplate;
        this.clock = clock;
    }

    @Override
    public void record(String keyword) {
        String normalized = normalize(keyword);
        if (normalized == null) {
            return;
        }
        try {
            ZSetOperations<String, String> sortedSets = redisTemplate.opsForZSet();
            sortedSets.add(RECENT_KEY, normalized, clock.millis());
            sortedSets.incrementScore(FREQUENCY_KEY, normalized, 1);
            Long size = sortedSets.zCard(RECENT_KEY);
            if (size != null && size > MAX_RECENT_TERMS) {
                sortedSets.removeRange(RECENT_KEY, 0, size - MAX_RECENT_TERMS - 1);
            }
        } catch (DataAccessException exception) {
            LOGGER.warn("Redis search history write unavailable");
        }
    }

    @Override
    public List<String> recent(int limit) {
        return reverseRange(RECENT_KEY, limit);
    }

    @Override
    public List<String> popular(int limit) {
        return reverseRange(FREQUENCY_KEY, limit);
    }

    private List<String> reverseRange(String key, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        try {
            Set<String> values = redisTemplate.opsForZSet().reverseRange(key, 0, safeLimit - 1L);
            return values == null ? List.of() : List.copyOf(values);
        } catch (DataAccessException exception) {
            LOGGER.warn("Redis search history read unavailable");
            return List.of();
        }
    }

    private String normalize(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}

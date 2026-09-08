package dev.blob.journal.service;

import dev.blob.journal.dto.JournalDetailResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisJournalCacheRepositoryTest {
    @Test
    @SuppressWarnings("unchecked")
    void corruptCacheIsTreatedAsMissSoMysqlCanRecover() {
        var redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenReturn("{broken");
        var repository = new RedisJournalCacheRepository(redis, JsonMapper.builder().build());

        assertThat(repository.get(42L)).isEmpty();
    }

    @Test
    void failedEvictionCannotReviveOldDetailWhenRedisRecovers() {
        var fixture = new RecoverableRedis();
        var repository = fixture.repository();
        repository.put(detail("旧标题"));
        assertThat(repository.get(42L)).get().extracting(JournalDetailResponse::title).isEqualTo("旧标题");

        fixture.available.set(false);
        // MySQL committed an update or deletion while Redis could not invalidate its copy.
        repository.evict(42L);
        fixture.available.set(true);

        assertThat(repository.get(42L)).isEmpty();
        repository.put(detail("MySQL 的新标题"));
        assertThat(repository.get(42L)).get().extracting(JournalDetailResponse::title).isEqualTo("MySQL 的新标题");
        assertThat(fixture.entries).containsEntry("unrelated:application:key", "keep");
    }

    @Test
    void backendRestartDoesNotReuseCacheFromBeforeAnOfflineMutation() {
        var fixture = new RecoverableRedis();
        var previousProcess = fixture.repository();
        previousProcess.put(detail("上次启动的旧标题"));
        fixture.available.set(false);
        previousProcess.evict(42L);
        fixture.available.set(true);

        var restartedProcess = fixture.repository();
        assertThat(restartedProcess.get(42L)).isEmpty();
    }

    private static JournalDetailResponse detail(String title) {
        return new JournalDetailResponse(42L, title, "正文", "LEARNING", LocalDate.of(2026, 9, 8),
                null, List.of(), null, null);
    }

    private static final class RecoverableRedis {
        private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
        private final Map<String, String> entries = new HashMap<>(Map.of("unrelated:application:key", "keep"));
        private final AtomicBoolean available = new AtomicBoolean(true);

        @SuppressWarnings("unchecked")
        private RecoverableRedis() {
            ValueOperations<String, String> values = mock(ValueOperations.class);
            when(redis.opsForValue()).thenReturn(values);
            when(values.get(anyString())).thenAnswer(invocation -> {
                requireConnection();
                return entries.get(invocation.getArgument(0, String.class));
            });
            doAnswer(invocation -> {
                requireConnection();
                entries.put(invocation.getArgument(0, String.class), invocation.getArgument(1, String.class));
                return null;
            }).when(values).set(anyString(), anyString(), any(Duration.class));
            when(redis.delete(anyString())).thenAnswer(invocation -> {
                requireConnection();
                return entries.remove(invocation.getArgument(0, String.class)) != null;
            });
        }

        private void requireConnection() {
            if (!available.get()) throw new RedisConnectionFailureException("test Redis unavailable");
        }

        private RedisJournalCacheRepository repository() {
            return new RedisJournalCacheRepository(redis, JsonMapper.builder().build());
        }
    }
}

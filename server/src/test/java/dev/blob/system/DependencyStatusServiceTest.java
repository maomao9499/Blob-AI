package dev.blob.system;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DependencyStatusServiceTest {

    @Test
    void reportsMysqlAndRedisIndependently() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        RedisConnectionFactory redisConnectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection redisConnection = mock(RedisConnection.class);
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        var statuses = new DependencyStatusService(jdbcTemplate, redisConnectionFactory).statuses();

        assertThat(statuses.get("mysql").status()).isEqualTo("UP");
        assertThat(statuses.get("redis").status()).isEqualTo("UP");
    }

    @Test
    void redisOutageDoesNotHideMysqlStatus() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        RedisConnectionFactory redisConnectionFactory = mock(RedisConnectionFactory.class);
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(redisConnectionFactory.getConnection()).thenThrow(new RedisConnectionFailureException("down"));

        var statuses = new DependencyStatusService(jdbcTemplate, redisConnectionFactory).statuses();

        assertThat(statuses.get("mysql").status()).isEqualTo("UP");
        assertThat(statuses.get("redis").status()).isEqualTo("DOWN");
        assertThat(statuses.get("redis").message()).doesNotContain("down");
    }
}

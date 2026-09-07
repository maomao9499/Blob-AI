package dev.blob.system;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DependencyStatusService {

    private final JdbcTemplate jdbcTemplate;
    private final RedisConnectionFactory redisConnectionFactory;

    public DependencyStatusService(
            JdbcTemplate jdbcTemplate,
            RedisConnectionFactory redisConnectionFactory
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    public Map<String, DependencyStatus> statuses() {
        Map<String, DependencyStatus> statuses = new LinkedHashMap<>();
        statuses.put("mysql", mysqlStatus());
        statuses.put("redis", redisStatus());
        return statuses;
    }

    private DependencyStatus mysqlStatus() {
        if (jdbcTemplate == null) {
            return new DependencyStatus("NOT_CONFIGURED", "MySQL 未配置");
        }
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Integer.valueOf(1).equals(result)
                    ? new DependencyStatus("UP", "MySQL 可用")
                    : new DependencyStatus("DOWN", "MySQL 响应异常");
        } catch (RuntimeException exception) {
            return new DependencyStatus("DOWN", "MySQL 连接不可用");
        }
    }

    private DependencyStatus redisStatus() {
        if (redisConnectionFactory == null) {
            return new DependencyStatus("NOT_CONFIGURED", "Redis 未配置");
        }
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            return "PONG".equalsIgnoreCase(connection.ping())
                    ? new DependencyStatus("UP", "Redis 可用")
                    : new DependencyStatus("DOWN", "Redis 响应异常");
        } catch (RuntimeException exception) {
            return new DependencyStatus("DOWN", "Redis 连接不可用");
        }
    }

    public record DependencyStatus(String status, String message) {
    }
}

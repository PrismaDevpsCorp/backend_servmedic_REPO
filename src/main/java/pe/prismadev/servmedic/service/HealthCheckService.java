package pe.prismadev.servmedic.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import pe.prismadev.servmedic.dto.HealthResponse;

import java.time.LocalDateTime;

@Service
public class HealthCheckService {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    public HealthCheckService(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    public HealthResponse check() {
        String databaseStatus = checkDatabase();
        String postgisStatus = checkPostgis();
        String redisStatus = checkRedis();

        String globalStatus = "OK".equals(databaseStatus)
            && postgisStatus.startsWith("OK")
            && "OK".equals(redisStatus)
            ? "OK"
            : "ERROR";

        return new HealthResponse(
            "ServMedic Backend",
            globalStatus,
            databaseStatus,
            postgisStatus,
            redisStatus,
            LocalDateTime.now()
        );
    }

    private String checkDatabase() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return result != null && result == 1 ? "OK" : "ERROR";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    private String checkPostgis() {
        try {
            String point = jdbcTemplate.queryForObject(
                "SELECT ST_AsText(ST_SetSRID(ST_MakePoint(-77.0428, -12.0464), 4326))",
                String.class
            );
            return point != null && point.startsWith("POINT") ? "OK: " + point : "ERROR";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    private String checkRedis() {
        try {
            redisTemplate.opsForValue().set("servmedic:health", "redis_ok");
            String value = redisTemplate.opsForValue().get("servmedic:health");
            return "redis_ok".equals(value) ? "OK" : "ERROR";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}

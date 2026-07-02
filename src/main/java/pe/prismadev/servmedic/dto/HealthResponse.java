package pe.prismadev.servmedic.dto;

import java.time.LocalDateTime;

public record HealthResponse(
    String app,
    String status,
    String database,
    String postgis,
    String redis,
    LocalDateTime timestamp
) {
}

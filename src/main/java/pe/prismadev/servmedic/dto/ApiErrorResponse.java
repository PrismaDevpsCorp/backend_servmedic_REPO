package pe.prismadev.servmedic.dto;

import java.time.LocalDateTime;

public record ApiErrorResponse(
    int status,
    String error,
    String message,
    String path,
    LocalDateTime timestamp
) {
}
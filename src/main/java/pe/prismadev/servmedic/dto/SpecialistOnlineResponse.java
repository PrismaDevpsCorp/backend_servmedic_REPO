package pe.prismadev.servmedic.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SpecialistOnlineResponse(
    Long specialistProfileId,
    String specialistFullName,
    String professionCode,
    boolean online,
    BigDecimal latitude,
    BigDecimal longitude,
    LocalDateTime timestamp,
    String message
) {
}
package pe.prismadev.servmedic.dto;

import java.time.LocalDateTime;

public record RequestOfferResponse(
    Long medicalRequestId,
    String requestCode,
    String requestStatus,
    Long specialistProfileId,
    String specialistFullName,
    String professionCode,
    String serviceCode,
    boolean offerActive,
    long ttlSeconds,
    LocalDateTime expiresAt,
    String message
) {
}
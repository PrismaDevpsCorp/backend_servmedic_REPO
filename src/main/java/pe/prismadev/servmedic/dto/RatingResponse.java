package pe.prismadev.servmedic.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RatingResponse(
    Long ratingId,
    Long medicalRequestId,
    String requestCode,
    String raterRole,
    int score,
    String comment,
    Long patientProfileId,
    String patientFullName,
    Long specialistProfileId,
    String specialistFullName,
    BigDecimal specialistRatingAverage,
    int specialistRatingCount,
    OffsetDateTime createdAt,
    String message
) {
}
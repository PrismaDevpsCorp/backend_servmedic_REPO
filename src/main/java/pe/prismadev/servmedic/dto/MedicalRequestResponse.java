package pe.prismadev.servmedic.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MedicalRequestResponse(
    Long id,
    String requestCode,
    Long patientProfileId,
    String patientFullName,
    String serviceCode,
    String serviceName,
    String professionCode,
    String professionName,
    boolean requiresPrescription,
    String status,
    Long acceptedSpecialistProfileId,
    String acceptedSpecialistFullName,
    String addressText,
    String addressReference,
    BigDecimal latitude,
    BigDecimal longitude,
    String prescriptionImageUrl,
    String patientNotes,
    BigDecimal estimatedAmount,
    BigDecimal distanceKm,
    OffsetDateTime createdAt,
    OffsetDateTime acceptedAt,
    OffsetDateTime startedRouteAt,
    OffsetDateTime startedAttentionAt,
    OffsetDateTime finishedAt
) {
}
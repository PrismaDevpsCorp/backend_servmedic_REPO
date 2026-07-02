package pe.prismadev.servmedic.dto;

import java.time.OffsetDateTime;

public record AttentionReportResponse(
    Long id,
    Long medicalRequestId,
    String requestCode,
    String requestStatus,
    Long patientProfileId,
    String patientFullName,
    Long specialistProfileId,
    String specialistFullName,
    String serviceCode,
    String serviceName,
    String clinicalObservations,
    String diagnosticImpression,
    String recommendations,
    String indications,
    String vitalSigns,
    String attachmentUrl,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
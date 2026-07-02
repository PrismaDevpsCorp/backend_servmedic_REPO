package pe.prismadev.servmedic.dto;

import java.math.BigDecimal;

public record AdminMedicalRequestResponse(
    Long requestId,
    String requestCode,
    String status,

    Long patientProfileId,
    String patientFullName,
    String patientMobilePhone,
    String patientEmail,

    String serviceCode,
    String serviceName,

    Long specialistProfileId,
    String specialistFullName,
    String specialistMobilePhone,

    BigDecimal estimatedAmount,
    String addressText,
    String addressReference,
    BigDecimal latitude,
    BigDecimal longitude,

    String paymentStatus,
    BigDecimal paidAmount,
    BigDecimal platformCommissionAmount,
    BigDecimal specialistNetAmount,
    String paymentMethod,

    String createdAt,
    String acceptedAt,
    String startedRouteAt,
    String startedAttentionAt,
    String finishedAt,
    String cancelledAt
) {
}
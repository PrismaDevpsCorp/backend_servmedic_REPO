package pe.prismadev.servmedic.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record NearbyMedicalRequestResponse(
    Long id,
    String requestCode,
    Long patientProfileId,
    String patientFullName,
    String serviceCode,
    String serviceName,
    String professionCode,
    String status,
    String addressText,
    BigDecimal latitude,
    BigDecimal longitude,
    BigDecimal distanceKm,
    BigDecimal estimatedAmount,
    OffsetDateTime createdAt
) {
}
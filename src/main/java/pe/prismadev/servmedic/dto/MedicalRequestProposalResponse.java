package pe.prismadev.servmedic.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MedicalRequestProposalResponse(

    Long proposalId,

    Long medicalRequestId,
    String requestCode,
    String requestStatus,

    Long specialistProfileId,
    String specialistFullName,

    String professionCode,
    String professionName,

    String serviceCode,
    String serviceName,

    BigDecimal serviceAmount,
    String mobilityPolicy,
    BigDecimal mobilityAmount,
    BigDecimal totalAmount,
    String currency,

    String message,
    Integer estimatedArrivalMinutes,

    String status,
    OffsetDateTime expiresAt,
    OffsetDateTime createdAt,
    OffsetDateTime respondedAt,
    OffsetDateTime withdrawnAt

) {
}

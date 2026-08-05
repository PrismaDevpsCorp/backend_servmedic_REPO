package pe.prismadev.servmedic.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MedicalRequestAdditionalResponse(

    Long additionalId,

    Long medicalRequestId,
    String requestCode,
    String requestStatus,

    Long specialistProfileId,
    String specialistFullName,

    String concept,
    String justification,

    BigDecimal amount,
    String currency,
    String status,

    BigDecimal originalTotalAmount,
    BigDecimal approvedAdditionalsAmount,
    BigDecimal currentTotalAmount,

    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    OffsetDateTime respondedAt,
    OffsetDateTime withdrawnAt

) {
}

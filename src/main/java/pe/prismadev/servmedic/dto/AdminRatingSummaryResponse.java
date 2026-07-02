package pe.prismadev.servmedic.dto;

import java.math.BigDecimal;

public record AdminRatingSummaryResponse(
    Long totalRatings,
    BigDecimal averageScore,
    Long patientToSpecialistRatings,
    Long specialistToPatientRatings,
    BigDecimal averagePatientToSpecialistScore,
    BigDecimal averageSpecialistToPatientScore
) {
}
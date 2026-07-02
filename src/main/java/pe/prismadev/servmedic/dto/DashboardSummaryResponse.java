package pe.prismadev.servmedic.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardSummaryResponse(
    Long totalPatients,
    Long totalSpecialists,
    Long activeSpecialists,
    Long pendingValidationSpecialists,
    Long pendingInterviewSpecialists,
    Long suspendedSpecialists,
    Long totalMedicalRequests,
    List<DashboardStatusCountResponse> medicalRequestsByStatus,
    Long totalPayments,
    BigDecimal grossAmount,
    BigDecimal platformCommissionAmount,
    BigDecimal specialistNetAmount,
    BigDecimal specialistAvailableWalletBalance,
    LocalDateTime generatedAt
) {
}
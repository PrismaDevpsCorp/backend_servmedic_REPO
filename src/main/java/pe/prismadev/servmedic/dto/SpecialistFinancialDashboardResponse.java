package pe.prismadev.servmedic.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record SpecialistFinancialDashboardResponse(
    Summary summary,
    List<Operation> operations,
    int page,
    int size,
    long totalElements,
    int totalPages
) {

    public record Summary(
        long totalOperations,
        long paidOperations,
        long pendingOperations,
        long rejectedOperations,
        BigDecimal paidServiceAmount,
        BigDecimal paidMobilityAmount,
        BigDecimal paidAdditionalAmount,
        BigDecimal paidTotalAmount,
        BigDecimal platformCommissionPercent,
        BigDecimal platformCommissionAmount,
        BigDecimal specialistNetAmount,
        String currency
    ) {
    }

    public record Operation(
        Long paymentId,
        Long medicalRequestId,
        String requestCode,
        String serviceName,
        String patientFullName,
        String paymentStatus,
        String paymentMethod,
        BigDecimal serviceAmount,
        BigDecimal mobilityAmount,
        BigDecimal additionalAmount,
        BigDecimal totalAmount,
        BigDecimal platformCommissionPercent,
        BigDecimal platformCommissionAmount,
        BigDecimal specialistNetAmount,
        String currency,
        String externalTransactionId,
        OffsetDateTime paidAt,
        OffsetDateTime createdAt
    ) {
    }
}

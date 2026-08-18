package pe.prismadev.servmedic.dto;

import java.math.BigDecimal;

public record AdminPaymentSummaryResponse(
    Long totalOperations,
    Long paidOperations,
    Long pendingOperations,
    Long rejectedOperations,

    BigDecimal paidServiceAmount,
    BigDecimal paidMobilityAmount,
    BigDecimal paidAdditionalAmount,
    BigDecimal paidTotalAmount,

    BigDecimal platformCommissionAmount,
    BigDecimal specialistNetAmount
) {
}
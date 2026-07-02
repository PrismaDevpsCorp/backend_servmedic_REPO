package pe.prismadev.servmedic.dto;

import java.math.BigDecimal;

public record AdminPaymentSummaryResponse(
    Long totalPayments,
    BigDecimal totalAmount,
    BigDecimal totalPlatformCommission,
    BigDecimal totalSpecialistNetAmount
) {
}
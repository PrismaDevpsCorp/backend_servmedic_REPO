package pe.prismadev.servmedic.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentResponse(
    Long paymentId,
    Long medicalRequestId,
    String requestCode,
    String paymentStatus,
    String paymentMethod,
    BigDecimal amount,
    BigDecimal platformCommissionPercent,
    BigDecimal platformCommissionAmount,
    BigDecimal specialistNetAmount,
    String currency,
    Long patientProfileId,
    String patientFullName,
    Long specialistProfileId,
    String specialistFullName,
    String externalTransactionId,
    OffsetDateTime paidAt,
    BigDecimal specialistAvailableBalance,
    String message
) {
}
package pe.prismadev.servmedic.dto;

import java.math.BigDecimal;

public record AdminPaymentResponse(
    Long paymentId,
    Long medicalRequestId,
    String requestCode,
    String requestStatus,

    Long patientProfileId,
    String patientFullName,
    String patientEmail,
    String patientMobilePhone,

    Long specialistProfileId,
    String specialistFullName,
    String specialistEmail,
    String specialistMobilePhone,

    BigDecimal amount,
    BigDecimal platformCommissionPercent,
    BigDecimal platformCommissionAmount,
    BigDecimal specialistNetAmount,
    String currency,
    String paymentMethod,
    String status,
    String externalTransactionId,

    String paidAt,
    String createdAt
) {
}
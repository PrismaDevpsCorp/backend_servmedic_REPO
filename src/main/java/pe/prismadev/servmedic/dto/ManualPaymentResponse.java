package pe.prismadev.servmedic.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ManualPaymentResponse(
    Long paymentId,
    Long medicalRequestId,
    String requestCode,
    String paymentFlow,
    String paymentStatus,
    String paymentMethod,
    BigDecimal serviceAmount,
    BigDecimal mobilityAmount,
    BigDecimal additionalAmount,
    BigDecimal totalAmount,
    BigDecimal commissionableAmount,
    BigDecimal platformCommissionPercent,
    BigDecimal platformCommissionAmount,
    BigDecimal specialistNetAmount,
    String currency,
    Long patientProfileId,
    String patientFullName,
    Long specialistProfileId,
    String specialistFullName,
    String externalTransactionId,
    boolean evidenceAvailable,
    String evidenceFileName,
    String evidenceContentType,
    Long evidenceSize,
    OffsetDateTime evidenceUploadedAt,
    OffsetDateTime paidAt,
    OffsetDateTime verifiedAt,
    OffsetDateTime rejectedAt,
    String rejectionReason,
    boolean verificationWarningAcknowledged,
    String message
) {
}

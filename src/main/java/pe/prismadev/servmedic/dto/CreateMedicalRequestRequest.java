package pe.prismadev.servmedic.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateMedicalRequestRequest(
    @NotNull Long patientProfileId,
    @NotBlank String serviceCode,
    @NotBlank String addressText,
    String addressReference,
    @NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal latitude,
    @NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal longitude,
    String prescriptionImageUrl,
    String patientNotes
) {
}
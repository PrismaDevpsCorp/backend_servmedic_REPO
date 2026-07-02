package pe.prismadev.servmedic.dto;

import jakarta.validation.constraints.NotBlank;

public record PayMedicalRequestRequest(
    @NotBlank String paymentMethod,
    String externalTransactionId
) {
}
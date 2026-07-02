package pe.prismadev.servmedic.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreatePatientRatingRequest(
    @NotNull Long patientProfileId,
    @Min(1) @Max(5) int score,
    String comment
) {
}
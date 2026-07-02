package pe.prismadev.servmedic.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateSpecialistRatingRequest(
    @NotNull Long specialistProfileId,
    @Min(1) @Max(5) int score,
    String comment
) {
}
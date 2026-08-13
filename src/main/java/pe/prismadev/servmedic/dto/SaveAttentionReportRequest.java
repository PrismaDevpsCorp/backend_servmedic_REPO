package pe.prismadev.servmedic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SaveAttentionReportRequest(
    @NotNull Long specialistProfileId,
    @NotBlank String clinicalObservations,
    String diagnosticImpression,
    @NotBlank String recommendations,
    @NotBlank(message = "Las indicaciones son obligatorias.")
    String indications,
    String vitalSigns,
    String attachmentUrl
) {
}

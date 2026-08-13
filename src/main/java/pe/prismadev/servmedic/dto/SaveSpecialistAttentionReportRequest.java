package pe.prismadev.servmedic.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveSpecialistAttentionReportRequest(
    @NotBlank String clinicalObservations,
    String diagnosticImpression,
    @NotBlank String recommendations,
    @NotBlank(message = "Las indicaciones son obligatorias.")
    String indications,
    String vitalSigns,
    String attachmentUrl
) {
}

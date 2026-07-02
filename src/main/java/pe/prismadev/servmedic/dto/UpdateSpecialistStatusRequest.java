package pe.prismadev.servmedic.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateSpecialistStatusRequest(
    @NotBlank String status
) {
}
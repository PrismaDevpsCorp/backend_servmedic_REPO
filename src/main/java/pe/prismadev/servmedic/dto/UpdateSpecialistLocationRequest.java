package pe.prismadev.servmedic.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateSpecialistLocationRequest(
    @NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal latitude,
    @NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal longitude
) {
}
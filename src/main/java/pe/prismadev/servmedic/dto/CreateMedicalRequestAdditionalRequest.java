package pe.prismadev.servmedic.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateMedicalRequestAdditionalRequest(

    @NotBlank
    @Size(min = 3, max = 120)
    String concept,

    @NotBlank
    @Size(min = 10, max = 1000)
    String justification,

    @NotNull
    @DecimalMin(value = "0.01")
    @Digits(integer = 8, fraction = 2)
    BigDecimal amount

) {
}

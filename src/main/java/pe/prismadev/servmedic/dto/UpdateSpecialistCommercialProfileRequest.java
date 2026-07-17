package pe.prismadev.servmedic.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UpdateSpecialistCommercialProfileRequest(
    @NotBlank
    @Pattern(
        regexp = "INCLUDED|SEPARATE|NOT_AVAILABLE",
        message = "La politica de movilidad debe ser INCLUDED, SEPARATE o NOT_AVAILABLE."
    )
    String mobilityPolicy,

    @DecimalMin(value = "0.01")
    @Digits(integer = 8, fraction = 2)
    BigDecimal mobilityReferenceAmount,

    @Size(max = 500)
    String commercialNotes,

    boolean active,

    @NotEmpty
    List<@Valid UpdateSpecialistServicePriceRequest> services,

    @NotEmpty
    List<
        @NotBlank
        @Size(max = 40)
        String
    > paymentMethodCodes
) {
}
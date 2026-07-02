package pe.prismadev.servmedic.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record CreateSpecialistRequest(
    @NotBlank @Email String email,
    @NotBlank String firstName,
    @NotBlank String lastName,
    @NotBlank @Pattern(regexp = "^[0-9]{8}$", message = "El DNI debe tener 8 digitos") String dni,
    String mobilePhone,
    String landlinePhone,
    @NotBlank String addressText,
    String addressReference,
    @NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal latitude,
    @NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal longitude,
    @NotBlank String professionCode,
    @NotBlank String collegeNumber,
    @NotEmpty List<String> offeredServiceCodes
) {
}
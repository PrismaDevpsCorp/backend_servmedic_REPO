package pe.prismadev.servmedic.dto;

import java.math.BigDecimal;

public record SpecialistServicePriceResponse(
    Long offeredServiceId,
    Long medicalServiceId,
    String serviceCode,
    String serviceName,
    boolean requiresPrescription,
    BigDecimal basePrice,
    boolean active
) {
}
package pe.prismadev.servmedic.dto;

import java.math.BigDecimal;
import java.util.List;

public record SpecialistCommercialProfileResponse(
    Long specialistProfileId,
    String professionCode,
    String professionName,
    String mobilityPolicy,
    BigDecimal mobilityReferenceAmount,
    String commercialNotes,
    boolean active,
    List<SpecialistServicePriceResponse> services,
    List<SpecialistPaymentMethodResponse> paymentMethods
) {
}
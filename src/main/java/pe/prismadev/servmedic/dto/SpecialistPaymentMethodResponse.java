package pe.prismadev.servmedic.dto;

public record SpecialistPaymentMethodResponse(
    Long paymentMethodId,
    String code,
    String name,
    boolean requiresVoucher,
    boolean selected
) {
}
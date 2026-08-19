package pe.prismadev.servmedic.dto;

public record SpecialistPaymentSettingResponse(
    Long paymentMethodId,
    String code,
    String name,
    boolean requiresVoucher,
    boolean selected,
    String mobilePhone,
    String accountHolder,
    String bankName,
    String accountNumber,
    String cci
) {
}

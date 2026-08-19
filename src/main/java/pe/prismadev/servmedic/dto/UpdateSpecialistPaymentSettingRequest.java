package pe.prismadev.servmedic.dto;

public record UpdateSpecialistPaymentSettingRequest(
    String code,
    boolean selected,
    String mobilePhone,
    String accountHolder,
    String bankName,
    String accountNumber,
    String cci
) {
}

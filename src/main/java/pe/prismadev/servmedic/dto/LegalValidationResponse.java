package pe.prismadev.servmedic.dto;

public record LegalValidationResponse(
    String professionCode,
    String serviceCode,
    boolean allowed,
    String message
) {
}
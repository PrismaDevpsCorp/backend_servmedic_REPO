package pe.prismadev.servmedic.dto;

public record PatientRegistrationResponse(
    Long userId,
    Long patientProfileId,
    String roleCode,
    String email,
    String fullName,
    String dni,
    String message
) {
}
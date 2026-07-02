package pe.prismadev.servmedic.dto;

public record UserLoginResponse(
    Long userId,
    String email,
    String fullName,
    String role,
    Long patientProfileId,
    Long specialistProfileId,
    String specialistStatus,
    String sessionToken,
    String startedAt
) {
}
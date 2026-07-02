package pe.prismadev.servmedic.dto;

public record AdminLoginResponse(
    Long userId,
    String email,
    String fullName,
    String role,
    String sessionToken,
    String startedAt
) {
}
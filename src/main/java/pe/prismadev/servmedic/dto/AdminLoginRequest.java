package pe.prismadev.servmedic.dto;

public record AdminLoginRequest(
    String username,
    String password
) {
}
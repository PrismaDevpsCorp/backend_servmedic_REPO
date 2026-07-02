package pe.prismadev.servmedic.dto;

public record UserLoginRequest(
    String username,
    String password
) {
}
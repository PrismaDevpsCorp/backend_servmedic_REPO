package pe.prismadev.servmedic.dto;

public record RoleResponse(
    Long id,
    String code,
    String name,
    boolean active
) {
}
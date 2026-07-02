package pe.prismadev.servmedic.dto;

public record ProfessionResponse(
    Long id,
    String code,
    String name,
    String collegeAcronym,
    boolean active
) {
}
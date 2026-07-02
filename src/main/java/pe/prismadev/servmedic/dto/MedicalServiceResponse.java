package pe.prismadev.servmedic.dto;

public record MedicalServiceResponse(
    Long id,
    String code,
    String name,
    boolean requiresPrescription,
    int displayOrder,
    boolean active,
    String professionCode,
    String professionName,
    String collegeAcronym
) {
}
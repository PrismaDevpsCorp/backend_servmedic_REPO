package pe.prismadev.servmedic.dto;

import java.util.List;

public record CatalogResponse(
    List<RoleResponse> roles,
    List<ProfessionResponse> professions,
    List<MedicalServiceResponse> medicalServices
) {
}
package pe.prismadev.servmedic.mapper;

import org.springframework.stereotype.Component;
import pe.prismadev.servmedic.dto.MedicalServiceResponse;
import pe.prismadev.servmedic.dto.ProfessionResponse;
import pe.prismadev.servmedic.dto.RoleResponse;
import pe.prismadev.servmedic.entity.MedicalService;
import pe.prismadev.servmedic.entity.Profession;
import pe.prismadev.servmedic.entity.Role;

@Component
public class CatalogMapper {

    public RoleResponse toRoleResponse(Role role) {
        return new RoleResponse(
            role.getId(),
            role.getCode(),
            role.getName(),
            role.isActive()
        );
    }

    public ProfessionResponse toProfessionResponse(Profession profession) {
        return new ProfessionResponse(
            profession.getId(),
            profession.getCode(),
            profession.getName(),
            profession.getCollegeAcronym(),
            profession.isActive()
        );
    }

    public MedicalServiceResponse toMedicalServiceResponse(MedicalService medicalService) {
        Profession profession = medicalService.getProfession();

        return new MedicalServiceResponse(
            medicalService.getId(),
            medicalService.getCode(),
            medicalService.getName(),
            medicalService.isRequiresPrescription(),
            medicalService.getDisplayOrder(),
            medicalService.isActive(),
            profession.getCode(),
            profession.getName(),
            profession.getCollegeAcronym()
        );
    }
}
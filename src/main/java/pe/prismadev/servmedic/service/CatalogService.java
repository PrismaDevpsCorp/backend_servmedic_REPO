package pe.prismadev.servmedic.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.prismadev.servmedic.dto.CatalogResponse;
import pe.prismadev.servmedic.dto.LegalValidationResponse;
import pe.prismadev.servmedic.dto.MedicalServiceResponse;
import pe.prismadev.servmedic.dto.ProfessionResponse;
import pe.prismadev.servmedic.dto.RoleResponse;
import pe.prismadev.servmedic.entity.MedicalService;
import pe.prismadev.servmedic.entity.Profession;
import pe.prismadev.servmedic.mapper.CatalogMapper;
import pe.prismadev.servmedic.repository.MedicalServiceRepository;
import pe.prismadev.servmedic.repository.ProfessionRepository;
import pe.prismadev.servmedic.repository.RoleRepository;

import java.util.List;

@Service
public class CatalogService {

    private final RoleRepository roleRepository;
    private final ProfessionRepository professionRepository;
    private final MedicalServiceRepository medicalServiceRepository;
    private final CatalogMapper catalogMapper;

    public CatalogService(
        RoleRepository roleRepository,
        ProfessionRepository professionRepository,
        MedicalServiceRepository medicalServiceRepository,
        CatalogMapper catalogMapper
    ) {
        this.roleRepository = roleRepository;
        this.professionRepository = professionRepository;
        this.medicalServiceRepository = medicalServiceRepository;
        this.catalogMapper = catalogMapper;
    }

    public List<RoleResponse> listRoles() {
        return roleRepository.findByActiveTrueOrderByIdAsc()
            .stream()
            .map(catalogMapper::toRoleResponse)
            .toList();
    }

    public List<ProfessionResponse> listProfessions() {
        return professionRepository.findByActiveTrueOrderByIdAsc()
            .stream()
            .map(catalogMapper::toProfessionResponse)
            .toList();
    }

    public List<MedicalServiceResponse> listAllMedicalServices() {
        return medicalServiceRepository.findAllActiveWithProfession()
            .stream()
            .map(catalogMapper::toMedicalServiceResponse)
            .toList();
    }

    public List<MedicalServiceResponse> listMedicalServicesByProfession(String professionCode) {
        validateProfessionExists(professionCode);

        return medicalServiceRepository.findActiveByProfessionCode(professionCode)
            .stream()
            .map(catalogMapper::toMedicalServiceResponse)
            .toList();
    }

    public CatalogResponse getFullCatalog() {
        return new CatalogResponse(
            listRoles(),
            listProfessions(),
            listAllMedicalServices()
        );
    }

    public LegalValidationResponse validateProfessionCanOfferService(String professionCode, String serviceCode) {
        Profession profession = validateProfessionExists(professionCode);

        MedicalService medicalService = medicalServiceRepository.findActiveByCode(serviceCode)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Servicio medico no encontrado o inactivo: " + serviceCode
            ));

        boolean allowed = medicalService.getProfession().getCode().equals(profession.getCode());

        String message = allowed
            ? "Servicio permitido para la profesion indicada."
            : "Servicio NO permitido para esta profesion. Regla antintrusismo aplicada por backend.";

        return new LegalValidationResponse(
            professionCode,
            serviceCode,
            allowed,
            message
        );
    }

    private Profession validateProfessionExists(String professionCode) {
        return professionRepository.findByCode(professionCode)
            .filter(Profession::isActive)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Profesion no encontrada o inactiva: " + professionCode
            ));
    }
}
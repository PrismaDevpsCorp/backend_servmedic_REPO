package pe.prismadev.servmedic.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.prismadev.servmedic.dto.CatalogResponse;
import pe.prismadev.servmedic.dto.LegalValidationResponse;
import pe.prismadev.servmedic.dto.MedicalServiceResponse;
import pe.prismadev.servmedic.dto.ProfessionResponse;
import pe.prismadev.servmedic.dto.RoleResponse;
import pe.prismadev.servmedic.service.CatalogService;

import java.util.List;

@RestController
@RequestMapping("/api/public/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/roles")
    public List<RoleResponse> listRoles() {
        return catalogService.listRoles();
    }

    @GetMapping("/professions")
    public List<ProfessionResponse> listProfessions() {
        return catalogService.listProfessions();
    }

    @GetMapping("/services")
    public List<MedicalServiceResponse> listServices(
        @RequestParam(required = false) String professionCode
    ) {
        if (professionCode == null || professionCode.isBlank()) {
            return catalogService.listAllMedicalServices();
        }

        return catalogService.listMedicalServicesByProfession(professionCode);
    }

    @GetMapping("/all")
    public CatalogResponse getFullCatalog() {
        return catalogService.getFullCatalog();
    }

    @GetMapping("/validate")
    public LegalValidationResponse validateLegalService(
        @RequestParam String professionCode,
        @RequestParam String serviceCode
    ) {
        return catalogService.validateProfessionCanOfferService(professionCode, serviceCode);
    }
}